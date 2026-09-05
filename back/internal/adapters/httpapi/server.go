package httpapi

import (
	"context"
	"encoding/json"
	"net/http"
	"strings"
	"time"

	"back/internal/domain"
)

type TargetProvider interface {
	CurrentTargets(ctx context.Context) ([]domain.TargetView, error)
}

type OpportunityProvider interface {
	CurrentOpportunities(ctx context.Context) ([]domain.Opportunity, error)
}

type Server struct {
	addr          string
	targets       TargetProvider
	opportunities OpportunityProvider
	allowedUsers  map[string]struct{}
}

func New(
	addr string,
	targets TargetProvider,
	opportunities OpportunityProvider,
	allowedUsers []string,
) *Server {
	server := &Server{
		addr:          addr,
		targets:       targets,
		opportunities: opportunities,
		allowedUsers:  make(map[string]struct{}),
	}
	for _, user := range allowedUsers {
		user = strings.ToLower(strings.TrimSpace(user))
		if user != "" {
			server.allowedUsers[user] = struct{}{}
		}
	}
	return server
}

func (s *Server) Run(ctx context.Context) error {
	server := &http.Server{
		Addr:              s.addr,
		Handler:           s.Handler(),
		ReadHeaderTimeout: 5 * time.Second,
		ReadTimeout:       15 * time.Second,
		WriteTimeout:      30 * time.Second,
		IdleTimeout:       60 * time.Second,
		MaxHeaderBytes:    16 << 10,
	}

	go func() {
		<-ctx.Done()

		shutdownCtx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
		defer cancel()

		_ = server.Shutdown(shutdownCtx)
	}()

	err := server.ListenAndServe()
	if err == http.ErrServerClosed {
		return nil
	}

	return err
}

func (s *Server) Handler() http.Handler {
	mux := http.NewServeMux()

	mux.HandleFunc("/api/v1/health", s.handleHealth)
	mux.Handle("/api/v1/targets", s.requireTailscaleIdentity(http.HandlerFunc(s.handleCurrentTargets)))
	mux.Handle("/api/v1/targets/current", s.requireTailscaleIdentity(http.HandlerFunc(s.handleCurrentTargets)))
	mux.Handle("/api/v1/opportunities", s.requireTailscaleIdentity(http.HandlerFunc(s.handleCurrentOpportunities)))
	mux.Handle("/api/v1/opportunities/current", s.requireTailscaleIdentity(http.HandlerFunc(s.handleCurrentOpportunities)))

	return noStore(mux)
}

func (s *Server) handleHealth(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		writeJSON(w, http.StatusMethodNotAllowed, map[string]any{"error": "method not allowed"})
		return
	}

	writeJSON(w, http.StatusOK, map[string]any{
		"ok":      true,
		"service": "dmtargetbot-api",
	})
}

func (s *Server) handleCurrentTargets(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		writeJSON(w, http.StatusMethodNotAllowed, map[string]any{
			"error": "method not allowed",
		})
		return
	}
	if s.targets == nil {
		writeJSON(w, http.StatusServiceUnavailable, map[string]any{"error": "target provider is not configured"})
		return
	}

	items, err := s.targets.CurrentTargets(r.Context())
	if err != nil {
		writeJSON(w, http.StatusInternalServerError, map[string]any{"error": "load targets"})
		return
	}

	writeJSON(w, http.StatusOK, map[string]any{
		"items": items,
	})
}

func (s *Server) handleCurrentOpportunities(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		writeJSON(w, http.StatusMethodNotAllowed, map[string]any{"error": "method not allowed"})
		return
	}
	if s.opportunities == nil {
		writeJSON(w, http.StatusServiceUnavailable, map[string]any{"error": "opportunity provider is not configured"})
		return
	}

	items, err := s.opportunities.CurrentOpportunities(r.Context())
	if err != nil {
		writeJSON(w, http.StatusInternalServerError, map[string]any{"error": "load opportunities"})
		return
	}

	writeJSON(w, http.StatusOK, map[string]any{"items": items})
}

func (s *Server) requireTailscaleIdentity(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if len(s.allowedUsers) == 0 {
			writeJSON(w, http.StatusServiceUnavailable, map[string]any{
				"error": "API identity allowlist is not configured",
			})
			return
		}

		login := strings.ToLower(strings.TrimSpace(r.Header.Get("Tailscale-User-Login")))
		if login == "" {
			writeJSON(w, http.StatusUnauthorized, map[string]any{
				"error": "missing Tailscale identity",
			})
			return
		}
		if _, ok := s.allowedUsers[login]; !ok {
			writeJSON(w, http.StatusForbidden, map[string]any{
				"error": "Tailscale user is not allowed",
			})
			return
		}

		next.ServeHTTP(w, r)
	})
}

func writeJSON(w http.ResponseWriter, status int, v any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(v)
}

func noStore(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Cache-Control", "no-store")
		next.ServeHTTP(w, r)
	})
}

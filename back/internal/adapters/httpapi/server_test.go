package httpapi

import (
	"context"
	"net/http"
	"net/http/httptest"
	"testing"

	"back/internal/domain"
)

type targetProviderStub struct{}

func (targetProviderStub) CurrentTargets(context.Context) ([]domain.TargetView, error) {
	return []domain.TargetView{{Title: "AK-47 | Test"}}, nil
}

type opportunityProviderStub struct{}

func (opportunityProviderStub) CurrentOpportunities(context.Context) ([]domain.Opportunity, error) {
	return []domain.Opportunity{{Title: "AK-47 | Test", Score: 42}}, nil
}

func TestHealthDoesNotRequireIdentity(t *testing.T) {
	server := New(":0", targetProviderStub{}, opportunityProviderStub{}, []string{"friend@github"})
	req := httptest.NewRequest(http.MethodGet, "/api/v1/health", nil)
	res := httptest.NewRecorder()

	server.Handler().ServeHTTP(res, req)
	if res.Code != http.StatusOK {
		t.Fatalf("status=%d body=%s", res.Code, res.Body.String())
	}
}

func TestDataEndpointsRequireAllowedTailscaleIdentity(t *testing.T) {
	server := New(":0", targetProviderStub{}, opportunityProviderStub{}, []string{"friend@github"})

	tests := []struct {
		name   string
		login  string
		status int
	}{
		{name: "missing", status: http.StatusUnauthorized},
		{name: "denied", login: "intruder@github", status: http.StatusForbidden},
		{name: "allowed", login: "FRIEND@GITHUB", status: http.StatusOK},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			req := httptest.NewRequest(http.MethodGet, "/api/v1/opportunities", nil)
			if tt.login != "" {
				req.Header.Set("Tailscale-User-Login", tt.login)
			}
			res := httptest.NewRecorder()

			server.Handler().ServeHTTP(res, req)
			if res.Code != tt.status {
				t.Fatalf("status=%d body=%s, want=%d", res.Code, res.Body.String(), tt.status)
			}
		})
	}
}

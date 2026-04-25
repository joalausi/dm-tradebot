# approximate structure:

```
back/
├─ cmd/
│  └─ dmarket-target-bot/
│     └─ main.go
│
├─ internal/
│  ├─ config/
│  │  ├─ load.go
│  │  └─ types.go
│  │
│  ├─ domain/
│  │  ├─ depth.go
│  │  └─ target.go
│  │
│  ├─ ports/
│  │  ├─ market_data.go
│  │  └─ notifier.go
│  │
│  ├─ services/
│  │  └─ dmarket_target_runner.go
│  │
│  └─ adapters/
│     ├─ markets/
│     │  └─ dmarket/
│     │     └─ client.go
│     │
│     └─ notifiers/
│        ├─ console/
│        │  └─ notifier.go
│        │
│        └─ discord/
│           └─ notifier.go
```
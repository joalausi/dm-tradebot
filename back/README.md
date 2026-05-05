# current structure:

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
│  │  ├─ target.go
│  │  └─ user_target.go
│  │
│  ├─ ports/
│  │  ├─ market_data.go
│  │  ├─ notifier.go
│  │  └─ user_target.go
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
│
├─ go.mod
├─ go.sum
├─ config.yaml
```

# useful links:

[SteamDT doc](https://doc.steamdt.com/)
[Dmarket doc](https://docs.dmarket.com/v1/swagger.html)
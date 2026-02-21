package core

import (
	"os"

	"gopkg.in/yaml.v3"
)

func LoadConfigFromFile(path string) (Config, error) {
	var cfg Config
	data, err := os.ReadFile(path)
	if err != nil {
		return cfg, err
	}
	if err := yaml.Unmarshal(data, &cfg); err != nil {
		return cfg, err
	}
	// дефолты
	if cfg.PollEvery == "" {
		cfg.PollEvery = "10m"
	}
	return cfg, nil
}

// func (c Config) DMarketKeys() (publicKey, secretKey string, err error) {
//     pkEnv := c.DMarket.PublicKeyEnv
//     skEnv := c.DMarket.SecretKeyEnv
//     if pkEnv == "" || skEnv == "" {
//         return "", "", fmt.Errorf("dmarket.public_key_env / secret_key_env not set in config")
//     }
//     publicKey = os.Getenv(pkEnv)
//     secretKey = os.Getenv(skEnv)
//     if publicKey == "" || secretKey == "" {
//         return "", "", fmt.Errorf("env %s or %s is empty", pkEnv, skEnv)
//     }
//     return publicKey, secretKey, nil
// }
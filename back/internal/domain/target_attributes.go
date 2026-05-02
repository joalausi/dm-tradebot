package domain

import (
	"sort"
	"strings"
)

var importantTargetAttrs = map[string]struct{}{
	"floatPartValue": {},
	"phase":          {},
	"paintSeed":      {},
}

func HasAdvancedAttributes(attrs []TargetAttribute) bool {
	for _, a := range attrs {
		if _, ok := importantTargetAttrs[a.Name]; ok && strings.TrimSpace(a.Value) != "" {
			return true
		}
	}
	return false
}

func TargetAttributesKey(attrs []TargetAttribute) string {
	if len(attrs) == 0 {
		return ""
	}

	parts := make([]string, 0, len(attrs))

	for _, a := range attrs {
		name := strings.TrimSpace(a.Name)
		value := strings.TrimSpace(a.Value)

		if name == "" || value == "" {
			continue
		}

		// only include attributes that are in important
		if _, ok := importantTargetAttrs[name]; !ok {
			continue
		}

		parts = append(parts, name+"="+value)
	}

	sort.Strings(parts)

	return strings.Join(parts, ";")
}

func PrettyTargetAttributes(attrs []TargetAttribute) string {
	key := TargetAttributesKey(attrs)
	if key == "" {
		return ""
	}
	return key
}

func TargetKey(gameID, title string, attrs []TargetAttribute) string {
	base := gameID + "|" + title

	attrKey := TargetAttributesKey(attrs)
	if attrKey == "" {
		return base
	}

	return base + "|" + attrKey
}

func ImportantAttributeMap(attrs []TargetAttribute) map[string]string {
	out := make(map[string]string)

	for _, a := range attrs {
		name := strings.TrimSpace(a.Name)
		value := strings.TrimSpace(a.Value)

		if name == "" || value == "" {
			continue
		}

		if _, ok := importantTargetAttrs[name]; !ok {
			continue
		}

		out[name] = value
	}

	return out
}

func SameImportantAttributes(want []TargetAttribute, got []TargetAttribute) bool {
	wantMap := ImportantAttributeMap(want)
	if len(wantMap) == 0 {
		return true
	}

	gotMap := ImportantAttributeMap(got)

	for name, wantValue := range wantMap {
		gotValue, ok := gotMap[name]
		if !ok {
			return false
		}

		if strings.TrimSpace(gotValue) != strings.TrimSpace(wantValue) {
			return false
		}
	}

	return true
}
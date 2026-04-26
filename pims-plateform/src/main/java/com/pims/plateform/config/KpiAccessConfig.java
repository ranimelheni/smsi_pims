package com.pims.plateform.config;

import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class KpiAccessConfig {

    // Matrice : rôle → liste des codes KPI accessibles
    private static final Map<String, Set<String>> ACCESS = Map.of(
        "rssi", Set.of(
            "employe_evalue",
            "participation_formation",
            "lecture_publication",
            "documents_valides",
            "conformite_soa"
        ),
        "direction", Set.of(
            "employe_evalue",
            "participation_formation",
            "lecture_publication",
            "documents_valides",
            "conformite_soa"
        ),
        "dpo", Set.of(
            "documents_valides",
            "conformite_soa"
        ),
        "auditeur", Set.of(
            "employe_evalue",
            "participation_formation",
            "lecture_publication",
            "documents_valides",
            "conformite_soa"
        ),
        "employe", Set.of(
            "participation_formation"   // uniquement son propre taux
        ),
        "admin_organism", Set.of(
            "employe_evalue",
            "participation_formation",
            "documents_valides"
        ),
        "super_admin", Set.of(
            "employe_evalue",
            "participation_formation",
            "lecture_publication",
            "documents_valides",
            "conformite_soa"
        )
    );

    public Set<String> getKpisForRole(String role) {
        return ACCESS.getOrDefault(role, Set.of());
    }

    public boolean canAccess(String role, String kpiCode) {
        return getKpisForRole(role).contains(kpiCode);
    }

    // Méta-données affichage par KPI
    public record KpiMeta(String label, String icon, String description) {}

    private static final Map<String, KpiMeta> META = Map.of(
        "employe_evalue",          new KpiMeta("Employés évalués",         "🎓", "Part des employés ayant reçu une évaluation de compétences"),
        "participation_formation", new KpiMeta("Participation formations",  "📚", "Taux de présence aux sessions terminées"),
        "lecture_publication",     new KpiMeta("Lecture des publications",  "📢", "Taux de lecture des communications publiées"),
        "documents_valides",       new KpiMeta("Documents validés",         "📄", "Part des documents approuvés sur l'ensemble non-brouillon"),
        "conformite_soa",          new KpiMeta("Conformité SoA",            "🔐", "Taux de contrôles ISO 27001 implémentés")
    );

    public KpiMeta getMeta(String code) {
        return META.getOrDefault(code,
            new KpiMeta(code, "📊", ""));
    }
}
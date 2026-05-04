export interface KpiResponse {
  organism_id:    number;
  organism_nom:   string;
  computed_at:    string;
  has_data:       boolean;
  message_vide:   string | null;
  soa:            SoaKpi | null;
  publications:   PublicationKpi | null;
  formation: FormationKpi | null;

}

export interface SoaKpi {
  has_data:         boolean;
  taux_global:      number;
  total_controles:  number;
  nb_implemente:    number;
  nb_en_cours:      number;
  nb_non_commence:  number;
  par_annexe:       SoaParAnnexe[];
  evolution:        SoaEvolutionPoint[];
}

export interface SoaParAnnexe {
  annexe:        string;
  annexe_label:  string;
  total_inclus:  number;
  nb_implemente: number;
  taux_annexe:   number;
}

export interface SoaEvolutionPoint {
  date:          string;
  taux_soa:      number;
  nb_implemente: number;
  total_inclus:  number;
}

// ── Publication — nouveau modèle simplifié ───────────────────────────
export interface PublicationKpi {
  has_data:            boolean;
  total_publications:  number;
  total_publiees:      number;
  total_users_actifs:  number;
  nb_lecteurs_uniques: number;
  total_lectures:      number;
  taux_lecture_global: number;
  par_publication:     PubParPublication[];
}

export interface PubParPublication {
  publication_id:    number;
  titre:             string;
  type:              string;
  priorite:          string;
  publie_le:         string;   // "YYYY-MM-DD"
  nb_lecteurs:       number;
  total_users_actifs:number;
  taux_lecture:      number;   // 0–100
}
// Dans KpiResponse ajouter :

// Nouveaux interfaces :
export interface FormationKpi {
  has_data:                  boolean;
  total_sessions:            number;
  sessions_terminees:        number;
  sessions_planifiees:       number;
  sessions_annulees:         number;
  total_inscriptions:        number;
  total_presents:            number;
  taux_participation_global: number;
  par_session:               FormationParSession[];
}

export interface FormationParSession {
  session_id:        number;
  titre:             string;
  type:              string;
  statut:            string;
  date_session:      string;
  max_participants:  number | null;
  nb_inscrits:       number;
  nb_presents:       number;
  taux_participation:number;
}
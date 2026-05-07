// Ajouter cette interface
export interface SessionResume {
  id:          number;
  titre:       string;
  norme:       string;
  date_debut:  string | null;
  date_fin:    string | null;
  total_nc:    number;
  non_traites: number;
}

// SuiviNc — supprimer audit_session_titre (inutile maintenant)
export interface SuiviNc {
  id:                number;
  clause_code:       string;
  clause_titre:      string;
  statut_audit:      string;
  justification:     string | null;
  action_planifiee:  string | null;
  priorite:          string;
  echeance_audit:    string | null;
  responsable_audit: string | null;
  statut_impl:       string;
  responsable_rssi:  string | null;
  echeance_rssi:     string | null;
  commentaire_rssi:  string | null;
  evalue_par:        string | null;
  evalue_at:         string | null;
  created_at:        string;
}

// SuiviNcKpi — inchangé
export interface SuiviNcKpi {
  has_data:        boolean;
  total_nc:        number;
  nb_non_conforme: number;
  nb_partiel:      number;
  nb_traites:      number;
  nb_en_cours:     number;
  nb_non_traites:  number;
  nb_reportes:     number;
  nb_acceptes:     number;
  taux_traitement: number;
  nb_en_retard:    number;
  par_clause:      SuiviNcParClause[];
}

export interface SuiviNcParClause {
  clause_principale: string;
  total:             number;
  nb_traites:        number;
  nb_non_traites:    number;
  taux_traitement:   number;
}
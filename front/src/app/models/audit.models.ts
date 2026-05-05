// audit.models.ts
export interface AuditSession {
  id: number;
  titre: string;
  norme: string;
  statut: string;
  version: string;
  date_debut: string;
  date_fin: string | null;
  auditeur: string;
  created_at: string;
}

export interface AuditContexte {
  organism_nom: string;
  perimetre_smsi: string | null;
  enjeux_externes: any[];
  enjeux_internes: any[];
  parties_interessees: any[];
  methode_risque: string | null;
  methode_statut: string | null;
  echelle_probabilite: number | null;
  echelle_impact: number | null;
  seuil_acceptable: number | null;
  seuil_eleve: number | null;
  labels_probabilite: string[];
  labels_impact: string[];
  soa_statut: string | null;
  soa_version: string | null;
  soa_nb_controles: number;
  soa_nb_implemente: number;
  soa_taux: number;
}

export interface AuditEvaluation {
  id: number | null;
  clause_code: string;
  clause_titre: string;
  clause_desc: string;
  parent_code: string | null;
  statut: string;
  justification: string | null;
  action_planifiee: string | null;
  priorite: string;
  echeance: string | null;
  responsable: string | null;
}

export interface AuditKpi {
  session_id: number;
  total_clauses_evaluees: number;
  nb_conforme: number;
  nb_non_conforme: number;
  nb_partiel: number;
  nb_planifie: number;
  nb_en_cours: number;
  nb_non_applicable: number;
  nb_non_evalue: number;
  taux_conformite_globale: number;
  taux_conformite_partielle: number;
  par_clause: ClauseKpi[];
}

export interface ClauseKpi {
  clause_principale: string;
  total: number;
  nb_conforme: number;
  nb_non_conforme: number;
  nb_partiel: number;
  taux_clause: number;
}
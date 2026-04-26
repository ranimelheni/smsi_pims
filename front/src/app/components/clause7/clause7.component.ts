import { Component, OnInit } from '@angular/core';
import { CommonModule }      from '@angular/common';
import { FormsModule }       from '@angular/forms';
import { Clause7Service }    from '../../services/clause7.service';
import { AuthService }       from '../../services/auth.service';
import { Router }            from '@angular/router';

@Component({
  selector:    'app-clause7',
  standalone:  true,
  imports:     [CommonModule, FormsModule],
  templateUrl: './clause7.component.html',
  styleUrls:   ['./clause7.component.scss']
})
export class Clause7Component implements OnInit {

  currentUser: any = null;
  activeTab = 'competences';

  // 7.1 Compétences
  monProfil:      any   = null;
  profils:        any[] = [];
  editProfil      = false;
  profilForm:     any   = {};
  evalForm:       any   = {};
  evalTarget:     any   = null;
  savingProfil    = false;

  // Détail employé sélectionné (RSSI)
  profilSelecte:  any   = null;

  // CV
  cvFile:         File | null = null;
  uploadingCv     = false;

  // Certifications
  showCertifForm  = false;
  certifForm:     any   = { nom: '', organisme: '', date_obtention: '', date_expiration: '' };
  certifFile:     File | null = null;
  savingCertif    = false;

  // 7.2 Formations
  sessions:           any[] = [];
  sessionSelectee:    any   = null;
  participants:       any[] = [];
  showNewSession      = false;
  sessionForm:        any   = { type: 'formation', mode: 'presentiel', obligatoire: false };
  evalPresenceForm:   any   = {};
  evalPresenceTarget: any   = null;

  // 7.3 Communication
  publications:   any[] = [];
  showNewPub      = false;
  pubForm:        any   = { type: 'information', priorite: 'normale', cible: 'tous', est_publie: false };
  pubSelectee:    any   = null;

  // 7.4 Documents
  documents:      any[] = [];
  showNewDoc      = false;
  docForm:        any   = { type_document: 'procedure', version: 'v1.0' };
  docFile:        File | null = null;
  filtreTypeDoc   = '';
  uploadingDoc    = false;

  loading  = false;
  success  = '';
  error    = '';

  typesSession = ['formation', 'sensibilisation', 'atelier'];
  modesSession = ['presentiel', 'distanciel', 'hybride'];
  typesPub     = ['information', 'alerte', 'procedure', 'politique', 'note'];
  prioritesPub = ['basse', 'normale', 'haute', 'urgente'];
  ciblesPub    = ['tous', 'employe', 'rssi', 'direction', 'dpo'];
  typesDoc     = ['procedure', 'politique', 'enregistrement', 'formulaire', 'guide', 'autre'];

  // ── Onglets dynamiques ─────────────────────────────────────────────────
get tabs() {
  return [
    { key: 'competences',   icon: '🎓', label: 'Compétences' },
    { key: 'formations',    icon: '📚', label: 'Sensibilisation' },
    { key: 'communication', icon: '📢', label: 'Communication' },
    { key: 'documents',     icon: '📄', label: 'Informations documentées' }
  ];
}

  constructor(
    private svc:    Clause7Service,
    private auth:   AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.currentUser = this.auth.getCurrentUser();
    this.loadTab('competences');
  }

  // ── Permissions ────────────────────────────────────────────────────────
  get canPublish():         boolean { return ['rssi','direction','admin_organism','super_admin'].includes(this.currentUser?.role); }
  get canEvaluerPresence(): boolean { return ['rssi','super_admin'].includes(this.currentUser?.role); }
  get canApprouverDoc():    boolean { return ['rssi','super_admin'].includes(this.currentUser?.role); }
  get canVoirDocuments():   boolean { return ['rssi','direction','dpo','admin_organism','employe','super_admin','pilote_processus'].includes(this.currentUser?.role); }
  get canDeposerDoc():      boolean { return ['rssi','direction','dpo','admin_organism','employe','super_admin','pilote_processus'].includes(this.currentUser?.role); }
  get isRssi():             boolean { return ['rssi','super_admin'].includes(this.currentUser?.role); }

  get canVoirProfils():    boolean { return ['rssi','admin_organism','super_admin','direction'].includes(this.currentUser?.role); }
get canEvaluerEmploye(): boolean { return ['rssi','admin_organism','super_admin'].includes(this.currentUser?.role); }
get isEmploye():         boolean { return ['employe','dpo', 'pilote_processus'].includes(this.currentUser?.role); }
get isDirection():       boolean { return this.currentUser?.role === 'direction'; }
  // ── Navigation ─────────────────────────────────────────────────────────
  setTab(key: string): void { this.activeTab = key; this.loadTab(key); }

  loadTab(key: string): void {
    this.loading = true;
    switch (key) {
      case 'competences':   this.loadCompetences();    break;
      case 'formations':    this.loadFormations();     break;
      case 'communication': this.loadCommunications(); break;
      case 'documents':     this.loadDocuments();      break;
      default: this.loading = false;
    }
  }

  // ════════════════════════════════════════════════════════════════
  // 7.1 COMPÉTENCES
  // ════════════════════════════════════════════════════════════════
loadCompetences(): void {
  if (!this.isDirection) {
    this.svc.getMonProfil().subscribe({
      next: (p) => {
        this.monProfil  = p;
        this.profilForm = {
          poste:       p.poste       || '',
          departement: p.departement || '',
          telephone:   p.telephone   || '',
          bio:         p.bio         || '',
          date_entree: p.date_entree || ''
        };
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  } else {
    this.loading = false;
  }

  if (this.canVoirProfils) {
    this.svc.getProfilsOrganism().subscribe({
      next: (l) => {
        if (this.isDirection) {
          // Direction : uniquement les profils évalués (employe + dpo)
          this.profils = l.filter((p: any) => p.statut_evaluation === 'evalue');
        } else {
          // RSSI/Admin : exclure direction ET son propre compte
          this.profils = l.filter((p: any) =>
            p.role !== 'direction' &&
            p.user_id !== this.currentUser?.id
          );
        }
      },
      error: () => {}
    });
  }
}

  saveProfil(): void {
    this.savingProfil = true;
    this.svc.updateMonProfil(this.profilForm).subscribe({
      next: (p) => {
        this.monProfil    = p;
        this.editProfil   = false;
        this.savingProfil = false;
        this.showSuccess('Profil mis à jour');
      },
      error: () => { this.savingProfil = false; }
    });
  }

  // ── Détail profil employé (RSSI) ────────────────────────────────────────
  voirProfilEmploye(p: any): void {
    this.profilSelecte = this.profilSelecte?.id === p.id ? null : p;
  }

  fermerProfilSelecte(): void { this.profilSelecte = null; }

  // ── CV ──────────────────────────────────────────────────────────────────
  onCvSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    const file    = input.files[0];
    const erreur  = this.validerFichier(file, ['pdf','doc','docx'], 10);
    if (erreur) { this.error = erreur; setTimeout(() => this.error = '', 4000); return; }
    this.cvFile = file;
  }

  uploadCv(): void {
    if (!this.cvFile) return;
    this.uploadingCv = true;
    this.svc.uploadCv(this.cvFile).subscribe({
      next: (p) => {
        this.monProfil   = p;
        this.cvFile      = null;
        this.uploadingCv = false;
        this.showSuccess('✅ CV téléversé');
      },
      error: (err) => {
        this.uploadingCv = false;
        this.error = err.error?.error || 'Erreur upload CV';
        setTimeout(() => this.error = '', 4000);
      }
    });
  }

  supprimerCv(): void {
    if (!confirm('Supprimer le CV ?')) return;
    this.svc.supprimerCv().subscribe({
      next: (p) => { this.monProfil = p; this.showSuccess('CV supprimé'); }
    });
  }

telechargerCv(userId: number, nomFichier: string): void {
  const id = userId ?? this.profilSelecte?.user_id ?? this.profilSelecte?.id;
  if (!id) { this.error = 'Identifiant introuvable'; return; }
  this.svc.downloadCv(id).subscribe({
    next: (blob) => this.declencherTelechargement(blob, nomFichier || 'cv'),
    error: () => { this.error = 'Erreur téléchargement'; setTimeout(() => this.error = '', 4000); }
  });
}
  // ── Certifications ──────────────────────────────────────────────────────
  ouvrirAjoutCertif(): void {
    this.showCertifForm = true;
    this.certifForm     = { nom: '', organisme: '', date_obtention: '', date_expiration: '' };
    this.certifFile     = null;
  }

  onCertifFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    const file   = input.files[0];
    const erreur = this.validerFichier(file, ['pdf','jpg','jpeg','png'], 5);
    if (erreur) { this.error = erreur; setTimeout(() => this.error = '', 4000); return; }
    this.certifFile = file;
  }

  ajouterCertif(): void {
    if (!this.certifForm.nom?.trim()) {
      this.error = 'Le nom de la certification est requis';
      setTimeout(() => this.error = '', 3000);
      return;
    }
    this.savingCertif = true;
    this.svc.ajouterCertification(this.certifForm, this.certifFile || undefined).subscribe({
      next: (p) => {
        this.monProfil      = p;
        this.showCertifForm = false;
        this.certifForm     = { nom: '', organisme: '', date_obtention: '', date_expiration: '' };
        this.certifFile     = null;
        this.savingCertif   = false;
        this.showSuccess('✅ Certification ajoutée');
      },
      error: (err) => {
        this.savingCertif = false;
        this.error = err.error?.error || 'Erreur';
        setTimeout(() => this.error = '', 4000);
      }
    });
  }

  supprimerCertification(id: number): void {
    if (!confirm('Supprimer cette certification ?')) return;
    this.svc.supprimerCertification(id).subscribe({
      next: (p) => { this.monProfil = p; this.showSuccess('Certification supprimée'); }
    });
  }

  telechargerCertif(certifId: number, nomFichier: string): void {
    this.svc.downloadCertif(certifId).subscribe({
      next: (blob) => this.declencherTelechargement(blob, nomFichier),
      error: () => { this.error = 'Erreur téléchargement'; }
    });
  }

  // ── Évaluation employé ──────────────────────────────────────────────────
  ouvrirEvaluation(p: any): void {
    if (!this.canEvaluerEmploye) return;
    this.evalTarget = p;
    this.evalForm   = {
      score_global:      p.score_global      || '',
      commentaire_admin: p.commentaire_admin || ''
    };
  }

  sauvegarderEvaluation(): void {
    if (!this.evalTarget || !this.evalForm.score_global) {
      this.error = 'Le score est requis';
      setTimeout(() => this.error = '', 3000);
      return;
    }
    this.svc.evaluerEmploye(this.evalTarget.id, this.evalForm).subscribe({
      next: (updated) => {
        const idx = this.profils.findIndex(p => p.id === updated.id);
        if (idx >= 0) this.profils[idx] = updated;
        if (this.profilSelecte?.id === updated.id) this.profilSelecte = updated;
        this.evalTarget = null;
        this.showSuccess('Évaluation enregistrée');
      },
      error: (err) => {
        this.error = err.error?.error || 'Erreur';
        setTimeout(() => this.error = '', 3000);
      }
    });
  }

  // ════════════════════════════════════════════════════════════════
  // 7.2 FORMATIONS
  // ════════════════════════════════════════════════════════════════
  loadFormations(): void {
    this.svc.getSessions().subscribe({
      next: (s) => { this.sessions = s; this.loading = false; },
      error: ()  => { this.loading = false; }
    });
  }

  creerSession(): void {
    if (!this.sessionForm.titre?.trim()) {
      this.error = 'Le titre est requis'; setTimeout(() => this.error = '', 3000); return;
    }
    if (!this.sessionForm.date_debut) {
      this.error = 'La date est requise'; setTimeout(() => this.error = '', 3000); return;
    }
    this.svc.creerSession(this.sessionForm).subscribe({
      next: (s) => {
        this.sessions.unshift(s);
        this.showNewSession = false;
        this.sessionForm    = { type: 'formation', mode: 'presentiel', obligatoire: false };
        this.showSuccess('✅ Session créée');
      },
      error: (err) => {
        this.error = err.error?.error || 'Erreur création';
        setTimeout(() => this.error = '', 4000);
      }
    });
  }

  sInscrire(s: any): void {
    this.svc.sInscrire(s.id).subscribe({
      next: () => { s.suis_inscrit = true; s.nb_inscrits++; this.showSuccess('Inscription confirmée'); },
      error: (e) => { this.error = e.error?.error || 'Erreur'; setTimeout(() => this.error = '', 3000); }
    });
  }

  seDesinscrire(s: any): void {
    this.svc.seDesinscrire(s.id).subscribe({
      next: () => { s.suis_inscrit = false; s.nb_inscrits = Math.max(0, s.nb_inscrits - 1); this.showSuccess('Désinscription'); }
    });
  }

  voirParticipants(s: any): void {
    this.sessionSelectee = s;
    this.svc.getParticipants(s.id).subscribe({ next: (p) => this.participants = p });
  }

  ouvrirEvalPresence(p: any): void {
    this.evalPresenceTarget = p;
    this.evalPresenceForm   = {
      statut:             p.statut,
      presence_confirmee: p.presence_confirmee,
      score_evaluation:   p.score_evaluation || '',
      commentaire_rssi:   p.commentaire_rssi || ''
    };
  }

  sauvegarderPresence(): void {
    if (!this.evalPresenceTarget) return;
    this.svc.evaluerPresence(this.evalPresenceTarget.id, this.evalPresenceForm).subscribe({
      next: (updated) => {
        const idx = this.participants.findIndex(p => p.id === updated.id);
        if (idx >= 0) this.participants[idx] = updated;
        this.evalPresenceTarget = null;
        this.showSuccess('Présence évaluée');
      },
      error: (err) => { this.error = err.error?.error || 'Erreur'; setTimeout(() => this.error = '', 3000); }
    });
  }

  // ════════════════════════════════════════════════════════════════
  // 7.3 COMMUNICATION
  // ════════════════════════════════════════════════════════════════
  loadCommunications(): void {
    this.svc.getPublications().subscribe({
      next: (p) => { this.publications = p; this.loading = false; },
      error: ()  => { this.loading = false; }
    });
  }

  creerPublication(): void {
    if (!this.pubForm.titre?.trim()) {
      this.error = 'Le titre est requis'; setTimeout(() => this.error = '', 3000); return;
    }
    if (!this.pubForm.contenu?.trim()) {
      this.error = 'Le contenu est requis'; setTimeout(() => this.error = '', 3000); return;
    }
    this.svc.creerPublication(this.pubForm).subscribe({
      next: (p) => {
        this.publications.unshift(p);
        this.showNewPub = false;
        this.pubForm    = { type: 'information', priorite: 'normale', cible: 'tous', est_publie: false };
        this.showSuccess('✅ Publication créée');
      },
      error: (err) => { this.error = err.error?.error || 'Erreur'; setTimeout(() => this.error = '', 4000); }
    });
  }

  publierCommunication(p: any): void {
    this.svc.publier(p.id).subscribe({
      next: (u) => {
        const idx = this.publications.findIndex(x => x.id === u.id);
        if (idx >= 0) this.publications[idx] = u;
        this.showSuccess('Publication diffusée');
      }
    });
  }

  marquerLu(p: any): void {
    if (p.lu) return;
    this.svc.marquerLu(p.id).subscribe({ next: () => { p.lu = true; p.nb_lectures++; } });
  }

  selectPub(p: any): void {
    this.pubSelectee = this.pubSelectee?.id === p.id ? null : p;
    if (this.pubSelectee) this.marquerLu(p);
  }

  // ════════════════════════════════════════════════════════════════
  // 7.4 DOCUMENTS
  // ════════════════════════════════════════════════════════════════
  loadDocuments(): void {
    if (!this.canVoirDocuments) { this.loading = false; return; }
    this.svc.getDocuments().subscribe({
      next: (d) => { this.documents = d; this.loading = false; },
      error: ()  => { this.loading = false; }
    });
  }

  get documentsFiltres(): any[] {
    return this.documents.filter(d =>
      !this.filtreTypeDoc || d.type_document === this.filtreTypeDoc
    );
  }

  onDocFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    const file   = input.files[0];
    const erreur = this.validerFichier(
      file, ['pdf','doc','docx','xls','xlsx','ppt','pptx','txt'], 10);
    if (erreur) { this.error = erreur; setTimeout(() => this.error = '', 4000); return; }
    this.docFile = file;
  }

  creerDocument(): void {
    if (!this.docForm.titre?.trim()) {
      this.error = 'Le titre est requis'; setTimeout(() => this.error = '', 3000); return;
    }
    this.uploadingDoc = true;
    this.svc.creerDocument(this.docForm, this.docFile || undefined).subscribe({
      next: (d) => {
        this.documents.unshift(d);   // ← ajoute en tête de liste immédiatement
        this.showNewDoc   = false;
        this.docFile      = null;
        this.docForm      = { type_document: 'procedure', version: 'v1.0' };
        this.uploadingDoc = false;
        this.showSuccess('✅ Document créé');
      },
      error: (err) => {
        this.uploadingDoc = false;
        this.error = err.error?.error || 'Erreur création document';
        setTimeout(() => this.error = '', 4000);
      }
    });
  }

  approuverDocument(d: any): void {
    this.svc.approuverDocument(d.id).subscribe({
      next: (updated) => {
        const idx = this.documents.findIndex(x => x.id === updated.id);
        if (idx >= 0) this.documents[idx] = updated;
        this.showSuccess('Document approuvé');
      }
    });
  }

  telechargerDocument(docId: number, nomFichier: string): void {
    this.svc.downloadDocument(docId).subscribe({
      next: (blob) => this.declencherTelechargement(blob, nomFichier),
      error: () => { this.error = 'Erreur téléchargement'; }
    });
  }

  // ── Helpers ─────────────────────────────────────────────────────────────
  private declencherTelechargement(blob: Blob, nomFichier: string): void {
    const url = URL.createObjectURL(blob);
    const a   = document.createElement('a');
    a.href    = url;
    a.download = nomFichier;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }

  private validerFichier(
      file: File,
      exts: string[],
      maxMo: number
  ): string | null {
    const ext = file.name.split('.').pop()?.toLowerCase() || '';
    if (!exts.includes(ext))
      return `Extension non autorisée. Formats : ${exts.join(', ')}`;
    if (file.size > maxMo * 1024 * 1024)
      return `Fichier trop volumineux (max ${maxMo} Mo)`;
    return null;
  }

  formatTaille(bytes: number | null): string {
    if (!bytes) return '';
    if (bytes < 1024)        return bytes + ' o';
    if (bytes < 1024 * 1024) return Math.round(bytes / 1024) + ' Ko';
    return (bytes / (1024 * 1024)).toFixed(1) + ' Mo';
  }

  formatDate(d: string): string {
    if (!d) return '—';
    return d.slice(0, 16).replace('T', ' ');
  }

  getFileIcon(nom: string | null): string {
    if (!nom) return '📎';
    const ext = nom.split('.').pop()?.toLowerCase();
    const m: Record<string, string> = {
      pdf:'📕', doc:'📘', docx:'📘', xls:'📗', xlsx:'📗',
      ppt:'📙', pptx:'📙', jpg:'🖼️', jpeg:'🖼️', png:'🖼️', txt:'📄'
    };
    return m[ext || ''] || '📎';
  }

  isCertifExpired(d: string):      boolean { return !!d && new Date(d) < new Date(); }
  isCertifExpiringSoon(d: string): boolean {
    if (!d) return false;
    const diff = new Date(d).getTime() - Date.now();
    return diff > 0 && diff < 90 * 24 * 60 * 60 * 1000;
  }

  getStatutEvalClass(s: string): string {
    return ({ non_evalue:'eval-non', en_cours:'eval-cours', evalue:'eval-ok' })[s] || '';
  }
  getStatutEvalLabel(s: string): string {
    return ({ non_evalue:'Non évalué', en_cours:'En cours', evalue:'Évalué' })[s] || s;
  }
  getPrioriteClass(p: string): string {
    return ({ basse:'prio-basse', normale:'prio-normale', haute:'prio-haute', urgente:'prio-urgente' })[p] || '';
  }
  getTypeDocIcon(t: string): string {
    return ({ procedure:'📋', politique:'📜', enregistrement:'📝', formulaire:'📃', guide:'📖', autre:'📄' })[t] || '📄';
  }
  getStatutDocClass(s: string): string {
    return ({ brouillon:'doc-brouillon', en_revision:'doc-revision', approuve:'doc-approuve', obsolete:'doc-obsolete' })[s] || '';
  }
  getPresenceClass(s: string): string {
    return ({ inscrit:'pr-inscrit', present:'pr-present', absent:'pr-absent', excuse:'pr-excuse' })[s] || '';
  }

  private showSuccess(msg: string): void {
    this.success = msg; setTimeout(() => this.success = '', 3000);
  }
  get isRssiOrAdmin(): boolean {
  return ['rssi', 'admin_organism', 'super_admin'].includes(this.currentUser?.role);
}
}
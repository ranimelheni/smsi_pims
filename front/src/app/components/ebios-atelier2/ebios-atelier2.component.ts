// ebios-atelier2.component.ts
import { Component, OnInit, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule, FormBuilder,
         FormGroup, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-ebios-atelier2',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './ebios-atelier2.component.html',
  styleUrls:  ['./ebios-atelier2.component.scss']
})
export class EbiosAtelier2Component implements OnInit {

  @Input() analyse: any = null;

  data:    any = null;
  loading  = false;
  success  = '';
  error    = '';

  // Vues
  activeView: 'sources' | 'couples' | 'synthese' = 'sources';

  // Formulaires
  srForm!: FormGroup;
  ovForm!: FormGroup;

  // États
  showSrForm    = false;
  showOvForm    = false;
  editingSr:    any = null;
  editingOv:    any = null;
  selectedSr:   any = null; // Source sélectionnée pour ajouter OV

  // Filtres synthèse
  filtreRetenu = '';

  private api = 'http://localhost:8080/api/ebios';

  categoriesSR = [
    { value: 'externe',     label: 'Externe',        icon: '🌐', desc: 'Acteur extérieur à l\'organisme' },
    { value: 'interne',     label: 'Interne',         icon: '🏢', desc: 'Personnel ou entité interne' },
    { value: 'etatique',    label: 'Étatique',        icon: '🏛️', desc: 'État, agence gouvernementale' },
    { value: 'criminel',    label: 'Criminel',        icon: '💀', desc: 'Organisation criminelle, hackers' },
    { value: 'concurrent',  label: 'Concurrent',      icon: '🎯', desc: 'Concurrent ou acteur hostile' },
    { value: 'accidentel',  label: 'Accidentel',      icon: '⚠️', desc: 'Erreur humaine non intentionnelle' },
    { value: 'naturel',     label: 'Naturel',         icon: '🌊', desc: 'Catastrophe naturelle' }
  ];

  niveauxLabels = ['—', 'Faible', 'Moyen', 'Élevé', 'Très élevé'];

  constructor(private fb: FormBuilder, private http: HttpClient) {}

  ngOnInit(): void {
    this.initForms();
    this.load();
  }

  initForms(): void {
    this.srForm = this.fb.group({
      libelle:     ['', Validators.required],
      categorie:   ['externe'],
      description: [''],
      retenu:      [true]
    });

    this.ovForm = this.fb.group({
      libelle:             ['', Validators.required],
      source_id:           ['', Validators.required],
      description:         [''],
      motivation:          [2],
      ressource:           [2],
      activite:            [2],
      pertinence_proposee: [2],
      pertinence_retenue:  [2],
      retenu:              [true],
      justification_rejet: ['']
    });
  }

  load(): void {
    this.loading = true;
    this.http.get<any>(`${this.api}/atelier2`).subscribe({
      next: (d) => { this.data = d; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  // ── Sources de risque ─────────────────────────────────────────────────────
  submitSR(): void {
    if (this.srForm.invalid) { this.srForm.markAllAsTouched(); return; }
    const url    = this.editingSr
      ? `${this.api}/sources-risque/${this.editingSr.id}`
      : `${this.api}/sources-risque`;
    const method = this.editingSr ? 'put' : 'post';

    (this.http as any)[method](url, this.srForm.value).subscribe({
      next: () => {
        this.showSrForm = false;
        this.editingSr  = null;
        this.srForm.reset({ categorie: 'externe', retenu: true });
        this.success = 'Source de risque enregistrée';
        this.load();
        setTimeout(() => this.success = '', 3000);
      },
      error: (err: any) => { this.error = err.error?.error || 'Erreur'; }
    });
  }

  editSR(sr: any): void {
    this.editingSr  = sr;
    this.showSrForm = true;
    this.srForm.patchValue(sr);
  }

  deleteSR(id: number): void {
    if (!confirm('Supprimer cette source ? Les objectifs visés associés seront aussi supprimés.')) return;
    this.http.delete(`${this.api}/sources-risque/${id}`).subscribe({
      next: () => this.load()
    });
  }

  toggleRetenuSR(sr: any): void {
    this.http.put(`${this.api}/sources-risque/${sr.id}`, {
      ...sr, retenu: !sr.retenu
    }).subscribe({ next: () => this.load() });
  }

  // ── Objectifs visés ───────────────────────────────────────────────────────
  openAddOV(sr: any): void {
    this.selectedSr = sr;
    this.editingOv  = null;
    this.showOvForm = true;
    this.ovForm.reset({
      source_id:   sr.id,
      motivation:  2, ressource: 2, activite: 2,
      pertinence_proposee: 2, pertinence_retenue: 2,
      retenu: true
    });
  }

  submitOV(): void {
    if (this.ovForm.invalid) { this.ovForm.markAllAsTouched(); return; }
    const url    = this.editingOv
      ? `${this.api}/objectifs-vises/${this.editingOv.id}`
      : `${this.api}/objectifs-vises`;
    const method = this.editingOv ? 'put' : 'post';

    // Synchroniser pertinence_retenue sur proposee si non modifiée
    const val = this.ovForm.value;
    if (!this.editingOv) val.pertinence_retenue = val.pertinence_proposee;

    (this.http as any)[method](url, val).subscribe({
      next: () => {
        this.showOvForm = false;
        this.editingOv  = null;
        this.selectedSr = null;
        this.ovForm.reset({ motivation:2, ressource:2, activite:2, pertinence_proposee:2, pertinence_retenue:2, retenu:true });
        this.success = 'Couple SR/OV enregistré';
        this.load();
        setTimeout(() => this.success = '', 3000);
      },
      error: (err: any) => { this.error = err.error?.error || 'Erreur'; }
    });
  }

  editOV(ov: any): void {
    this.editingOv  = ov;
    this.showOvForm = true;
    this.selectedSr = this.getSRById(ov.source_id);
    this.ovForm.patchValue(ov);
  }

  deleteOV(id: number): void {
    if (!confirm('Supprimer ce couple SR/OV ?')) return;
    this.http.delete(`${this.api}/objectifs-vises/${id}`).subscribe({
      next: () => this.load()
    });
  }

  toggleRetenuOV(ov: any): void {
    const newRetenu = !ov.retenu;
    this.http.put(`${this.api}/objectifs-vises/${ov.id}`, {
      retenu: newRetenu,
      justification_rejet: newRetenu ? null : 'Non retenu'
    }).subscribe({ next: () => this.load() });
  }

  // ── Pertinence auto-calcul ────────────────────────────────────────────────
  onPertinenceChange(): void {
    const v = this.ovForm.value;
    const calc = Math.max(v.motivation, v.ressource, v.activite);
    this.ovForm.patchValue({ pertinence_proposee: calc }, { emitEvent: false });
    if (!this.editingOv) {
      this.ovForm.patchValue({ pertinence_retenue: calc }, { emitEvent: false });
    }
  }

  // ── Helpers ───────────────────────────────────────────────────────────────
  get listeSR():       any[] { return this.data?.sources_risque  || []; }
  get listeOV():       any[] { return this.data?.objectifs_vises || []; }
  get listeOVRetenu(): any[] { return this.listeOV.filter(o => o.retenu); }

  getOVForSR(srId: number): any[] {
    return this.data?.ov_par_source?.[srId] || [];
  }

  getSRById(id: number): any {
    return this.listeSR.find(s => s.id === id);
  }

  getCatInfo(v: string) {
    return this.categoriesSR.find(c => c.value === v) ||
      { icon: '❓', label: v, desc: '' };
  }

  getNiveauLabel(n: number): string { return this.niveauxLabels[n] || '—'; }

  getNiveauClass(n: number): string {
    const m: Record<number,string> = {
      1: 'niveau-1', 2: 'niveau-2', 3: 'niveau-3', 4: 'niveau-4'
    };
    return m[n] || '';
  }

  getPertinenceClass(n: number): string {
    if (n <= 1) return 'pert-faible';
    if (n === 2) return 'pert-moyen';
    if (n === 3) return 'pert-eleve';
    return 'pert-critique';
  }

  get statsText(): string {
    const s = this.data?.stats;
    if (!s) return '';
    return `${s.sources_retenues}/${s.sources_total} sources retenues · ${s.couples_retenus}/${s.couples_total} couples retenus`;
  }

  get listeOVFiltre(): any[] {
    if (!this.filtreRetenu) return this.listeOV;
    return this.listeOV.filter(o =>
      this.filtreRetenu === 'retenu' ? o.retenu : !o.retenu);
  }
}
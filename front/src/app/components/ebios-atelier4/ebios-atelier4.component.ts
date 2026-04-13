// ebios-atelier4.component.ts
import { Component, OnInit, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule, FormBuilder,
         FormGroup, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-ebios-atelier4',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './ebios-atelier4.component.html',
  styleUrls:  ['./ebios-atelier4.component.scss']
})
export class EbiosAtelier4Component implements OnInit {

  @Input() analyse: any = null;

  data:    any  = null;
  loading  = false;
  success  = '';
  error    = '';

  activeView: 'scenarios' | 'graphe' = 'scenarios';

  soForm!: FormGroup;
  aeForm!: FormGroup;

  showSoForm  = false;
  editingSo:  any = null;
  showAeForms: Record<number, boolean> = {};
  editingAe:  any = null;

  filtreNiveauSO = '';
  filtreSSId     = '';

  private api = 'http://localhost:8080/api/ebios';

  modesOp   = ['physique','logique','social','hybride'];
  canaux    = ['réseau','email','usb','web','application','physique','autre'];
  niveauxLabels = ['—','Faible','Moyen','Élevé'];
  typeBienIcons: Record<string,string> = {
    materiel:'🖥️', logiciel:'💿', reseau:'🌐',
    donnees:'🗄️', humain:'👤', site:'🏢', cloud:'☁️'
  };

  constructor(private fb: FormBuilder, private http: HttpClient) {}

  ngOnInit(): void {
    this.initForms();
    this.load();
  }

  initForms(): void {
    this.soForm = this.fb.group({
      libelle:                  ['', Validators.required],
      scenario_strategique_id:  ['', Validators.required],
      bien_support_id:          [''],
      description:              [''],
      canal_exfiltration:       [''],
      gravite:                  [2],
      vraisemblance:            [2]
    });

    this.aeForm = this.fb.group({
      libelle:           ['', Validators.required],
      scenario_op_id:    ['', Validators.required],
      numero:            [1],
      mode_operatoire:   ['logique'],
      probabilite_succes:[2],
      difficulte:        [2],
      prerequis:         [''],
      description:       ['']
    });
  }

  load(): void {
    this.loading = true;
    this.http.get<any>(`${this.api}/atelier4`).subscribe({
      next: (d) => { this.data = d; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  // ── Scénarios opérationnels ───────────────────────────────────────────────
  submitSO(): void {
    if (this.soForm.invalid) { this.soForm.markAllAsTouched(); return; }
    const url    = this.editingSo
      ? `${this.api}/scenarios-operationnels/${this.editingSo.id}`
      : `${this.api}/scenarios-operationnels`;
    const method = this.editingSo ? 'put' : 'post';

    (this.http as any)[method](url, this.soForm.value).subscribe({
      next: () => {
        this.showSoForm = false; this.editingSo = null;
        this.soForm.reset({ gravite:2, vraisemblance:2 });
        this.success = 'Scénario opérationnel enregistré';
        this.load();
        setTimeout(() => this.success = '', 3000);
      },
      error: (err: any) => { this.error = err.error?.error || 'Erreur'; }
    });
  }

  editSO(so: any): void {
    this.editingSo  = so;
    this.showSoForm = true;
    this.soForm.patchValue(so);
  }

  deleteSO(id: number): void {
    if (!confirm('Supprimer ce scénario opérationnel et ses actions élémentaires ?')) return;
    this.http.delete(`${this.api}/scenarios-operationnels/${id}`).subscribe({
      next: () => this.load()
    });
  }

  // ── Actions élémentaires ──────────────────────────────────────────────────
  openAddAE(soId: number, nextNum: number): void {
    this.showAeForms[soId] = true;
    this.editingAe = null;
    this.aeForm.reset({
      scenario_op_id:    soId,
      numero:            nextNum,
      mode_operatoire:   'logique',
      probabilite_succes:2,
      difficulte:        2
    });
  }

  submitAE(): void {
    if (this.aeForm.invalid) { this.aeForm.markAllAsTouched(); return; }
    const url    = this.editingAe
      ? `${this.api}/actions-elementaires/${this.editingAe.id}`
      : `${this.api}/actions-elementaires`;
    const method = this.editingAe ? 'put' : 'post';

    (this.http as any)[method](url, this.aeForm.value).subscribe({
      next: () => {
        const soId = this.aeForm.value.scenario_op_id;
        this.showAeForms[soId] = false;
        this.editingAe = null;
        this.aeForm.reset({ mode_operatoire:'logique', probabilite_succes:2, difficulte:2 });
        this.success = 'Action élémentaire enregistrée';
        this.load();
        setTimeout(() => this.success = '', 3000);
      },
      error: (err: any) => { this.error = err.error?.error || 'Erreur'; }
    });
  }

  editAE(ae: any, soId: number): void {
    this.editingAe = ae;
    this.showAeForms[soId] = true;
    this.aeForm.patchValue({ ...ae, scenario_op_id: soId });
  }

  deleteAE(id: number): void {
    if (!confirm('Supprimer cette action ?')) return;
    this.http.delete(`${this.api}/actions-elementaires/${id}`).subscribe({
      next: () => this.load()
    });
  }

  // ── Helpers ───────────────────────────────────────────────────────────────
  get listeSO():  any[] { return this.data?.scenarios_operationnels || []; }
  get listeSS():  any[] { return this.data?.scenarios_strategiques  || []; }
  get listeBS():  any[] { return this.data?.biens_support           || []; }

  getActionsForSO(soId: number): any[] {
    return this.data?.actions_par_so?.[soId] || [];
  }

  getNextNumero(soId: number): number {
    const actions = this.getActionsForSO(soId);
    return actions.length > 0 ? Math.max(...actions.map((a: any) => a.numero)) + 1 : 1;
  }

  get listeSoFiltre(): any[] {
    return this.listeSO.filter(s => {
      const matchN = !this.filtreNiveauSO || s.niveau_risque?.toString() === this.filtreNiveauSO;
      const matchS = !this.filtreSSId     || s.scenario_strategique_id?.toString() === this.filtreSSId;
      return matchN && matchS;
    });
  }

  getNiveauClass(n: number): string {
    return n === 1 ? 'niveau-faible' : n === 2 ? 'niveau-moyen' : n === 3 ? 'niveau-eleve' : '';
  }

  getNiveauLabel(n: number): string { return this.niveauxLabels[n] || '—'; }

  getModeIcon(m: string): string {
    const map: Record<string,string> = {
      physique:'🔧', logique:'💻', social:'👥', hybride:'🔀'
    };
    return map[m] || '❓';
  }

  getVraisemblanceActionLabel(v: number): string {
    const m: Record<number,string> = {1:'Très improbable',2:'Improbable',3:'Probable',4:'Quasi-certain'};
    return m[v] || '—';
  }

  calcPreviewVraisemblance(): number {
    const p = this.aeForm.get('probabilite_succes')?.value || 2;
    const d = this.aeForm.get('difficulte')?.value          || 2;
    const s = p * d;
    if (s <= 1)  return 1;
    if (s <= 4)  return 2;
    if (s <= 9)  return 3;
    return 4;
  }

  calcPreviewNiveau(): number {
    const g   = this.soForm.get('gravite')?.value       || 2;
    const v   = this.soForm.get('vraisemblance')?.value || 2;
    const acc = this.analyse?.seuil_acceptable          || 6;
    const elv = this.analyse?.seuil_eleve               || 12;
    const s = g * v;
    if (s <= acc) return 1;
    if (s <= elv) return 2;
    return 3;
  }

  getRange(n: number): number[] {
    return Array.from({ length: n }, (_, i) => i + 1);
  }

  getBSIcon(type: string): string {
    return this.typeBienIcons[type] || '📦';
  }

  // Vue graphe : grouper SO par SS
  get soParSS(): Record<number, any[]> {
    const grouped: Record<number, any[]> = {};
    this.listeSO.forEach(so => {
      const ssId = so.scenario_strategique_id;
      if (!grouped[ssId]) grouped[ssId] = [];
      grouped[ssId].push(so);
    });
    return grouped;
  }

  getSsKeys(): number[] { return Object.keys(this.soParSS).map(Number); }
  getSsById(id: number): any { return this.listeSS.find(s => s.id === id); }
  getGVClass(n: number, max: number): string {
  const ratio = n / max;
  if (ratio <= 0.25) return 'gv-1';
  if (ratio <= 0.5)  return 'gv-2';
  if (ratio <= 0.75) return 'gv-3';
  return 'gv-4';
}
}
import {
  Component, OnInit, OnDestroy,
  NgZone, ChangeDetectorRef,
  ChangeDetectionStrategy
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuditService } from '../../services/audit.service';
import { KpiService } from '../../services/kpi.service';
import { AuthService } from '../../services/auth.service';
import {
  AuditSession, AuditContexte, AuditEvaluation, AuditKpi
} from '../../models/audit.models';
import { KpiResponse } from '../../models/kpi.models';

import {
  Chart, RadarController, DoughnutController, BarController,
  RadialLinearScale, LinearScale, CategoryScale,
  ArcElement, BarElement, PointElement, LineElement,
  Tooltip, Legend
} from 'chart.js';

Chart.register(
  RadarController, DoughnutController, BarController,
  RadialLinearScale, LinearScale, CategoryScale,
  ArcElement, BarElement, PointElement, LineElement,
  Tooltip, Legend
);
Chart.defaults.animation = false as any;
Chart.defaults.responsive = true;
Chart.defaults.maintainAspectRatio = false;

type Tab = 'contexte' | 'kpi-rssi' | 'evaluation' | 'kpi-audit';

@Component({
  selector: 'app-auditeur-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './auditeur-dashboard.component.html',
  styleUrls: ['./auditeur-dashboard.component.scss']
})
export class AuditeurDashboardComponent implements OnInit, OnDestroy {

  currentUser: any = null;
  activeTab: Tab = 'contexte';

  // Sessions
  sessions: AuditSession[] = [];
  selectedSession: AuditSession | null = null;
  showNewSession = false;
  newSessionForm = { titre: 'Analyse des écarts ISO 27001', norme: 'iso27001' };
  creatingSess = false;

  // Contexte
  contexte: AuditContexte | null = null;
  loadingCtx = false;

  // KPI RSSI (existants)
  kpiRssi: KpiResponse | null = null;
  loadingKpiRssi = false;

  // Évaluations
  evaluations: AuditEvaluation[] = [];
  loadingEval = false;
  savingEval = new Set<string>();
  filtreStatutEval = '';
  filtreClause = '';

  // KPI Audit
  kpiAudit: AuditKpi | null = null;
  loadingKpiAudit = false;

  loading = false;
  success = '';
  error = '';

  private readonly uid = Date.now().toString(36);
  private charts = new Map<string, Chart>();
  private timers: ReturnType<typeof setTimeout>[] = [];

readonly chartIds = {
  auditRadar:    `aud-radar-${this.uid}`,
  auditDoughnut: `aud-doughnut-${this.uid}`,
  rssiSoaRadar:  `rssi-soa-${this.uid}`,
  rssiPubBar:    `rssi-pub-bar-${this.uid}`,    // ← remplace rssiPubRadar
  rssiFormBar:   `rssi-form-bar-${this.uid}`    // ← nouveau
};

  tabs = [
   
    { key: 'contexte', icon: '🏢', label: 'Contexte & Méthodologie' },
    { key: 'kpi-rssi', icon: '📊', label: 'KPI de l\'organisme' },
    { key: 'evaluation', icon: '✅', label: 'Tableau de conformité' },
    { key: 'kpi-audit', icon: '🎯', label: 'Analyse des écarts' }
  ];

  STATUTS = [
    { value: 'conforme', label: 'Conforme', color: '#10b981' },
    { value: 'non_conforme', label: 'Non conforme', color: '#ef4444' },
    { value: 'partiel', label: 'Partiel', color: '#f59e0b' },
    { value: 'planifie', label: 'Planifié', color: '#3b82f6' },
    { value: 'en_cours', label: 'En cours', color: '#8b5cf6' },
    { value: 'non_applicable', label: 'Non applicable', color: '#9ca3af' },
    { value: 'non_evalue', label: 'Non évalué', color: '#d1d5db' }
  ];

  PRIORITES = ['basse', 'normale', 'haute', 'critique'];

  constructor(
    private auditSvc: AuditService,
    private kpiSvc: KpiService,
    private auth: AuthService,
    private router: Router,
    private zone: NgZone,
    private cdr: ChangeDetectorRef
  ) { }

  ngOnInit(): void {
    this.currentUser = this.auth.getCurrentUser();
    this.loadSessions();
    this.loadContexte();
  }

  ngOnDestroy(): void {
    this.timers.forEach(t => clearTimeout(t));
    this.destroyAll();
  }

  // ── Navigation ────────────────────────────────────────────────
  setTab(tab: Tab | undefined): void {
    if (!tab) return;
    this.activeTab = tab;
    this.cdr.markForCheck();

    switch (tab) {
      case 'contexte': 
        if (!this.contexte) this.loadContexte(); 
        break;
      case 'kpi-rssi': 
        if (!this.kpiRssi) this.loadKpiRssi(); 
        else {
          this.zone.runOutsideAngular(() => {
            const t = setTimeout(() => {
              this.initRssiCharts();
              this.zone.run(() => this.cdr.markForCheck());
            }, 100);
            this.timers.push(t);
          });
        }
        break;
      case 'evaluation': 
        if (this.selectedSession) this.loadEvaluations(); 
        break;
      case 'kpi-audit': 
        if (this.selectedSession) this.loadKpiAudit(); 
        break;
    }
  }

  // ── Sessions ──────────────────────────────────────────────────
  loadSessions(): void {
    this.loading = true;
    this.cdr.markForCheck();
    this.auditSvc.getSessions().subscribe({
      next: (s) => {
        this.sessions = s;
        if (s.length) this.selectedSession = s[0];
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => { this.loading = false; this.cdr.markForCheck(); }
    });
  }

  selectSession(s: AuditSession): void {
    this.selectedSession = s;
    this.evaluations = [];
    this.kpiAudit = null;
    this.cdr.markForCheck();
    // Recharger si on est sur evaluation ou kpi-audit
    if (this.activeTab === 'evaluation') this.loadEvaluations();
    if (this.activeTab === 'kpi-audit') this.loadKpiAudit();
  }

  createSession(): void {
    if (!this.newSessionForm.titre.trim()) return;
    this.creatingSess = true;
    this.cdr.markForCheck();

    this.auditSvc.createSession(this.newSessionForm).subscribe({
      next: (s) => {
        this.creatingSess = false;
        this.showNewSession = false;
        if (s) {
          this.sessions.unshift(s);
          this.selectedSession = s;
        }
        this.showSuccess('Session créée');
        this.cdr.markForCheck();
      },
      error: () => { this.creatingSess = false; this.cdr.markForCheck(); }
    });
  }

  // ── Contexte ──────────────────────────────────────────────────
  loadContexte(): void {
    this.loadingCtx = true;
    this.cdr.markForCheck();
    this.auditSvc.getContexte().subscribe({
      next: (c) => { this.contexte = c; this.loadingCtx = false; this.cdr.markForCheck(); },
      error: () => { this.loadingCtx = false; this.cdr.markForCheck(); }
    });
  }

  // ── KPI RSSI ──────────────────────────────────────────────────
  loadKpiRssi(): void {
    this.loadingKpiRssi = true;
    this.cdr.markForCheck();
    this.kpiSvc.getDashboard(6).subscribe({
      next: (k) => {
        this.kpiRssi = k;
        this.loadingKpiRssi = false;
        this.cdr.markForCheck();
        this.zone.runOutsideAngular(() => {
          const t = setTimeout(() => {
            this.initRssiCharts();
            this.zone.run(() => this.cdr.markForCheck());
          }, 100);
          this.timers.push(t);
        });
      },
      error: () => { this.loadingKpiRssi = false; this.cdr.markForCheck(); }
    });
  }

  // ── Évaluations ───────────────────────────────────────────────
  loadEvaluations(): void {
    if (!this.selectedSession) return;
    this.loadingEval = true;
    this.cdr.markForCheck();
    this.auditSvc.getEvaluations(this.selectedSession.id).subscribe({
      next: (e) => { this.evaluations = e; this.loadingEval = false; this.cdr.markForCheck(); },
      error: () => { this.loadingEval = false; this.cdr.markForCheck(); }
    });
  }

  saveEvaluation(ev: AuditEvaluation): void {
    if (!this.selectedSession) return;
    this.savingEval.add(ev.clause_code);

    this.auditSvc.updateEvaluation(
      this.selectedSession.id,
      ev.clause_code,
      {
        statut: ev.statut,
        justification: ev.justification,
        action_planifiee: ev.action_planifiee,
        priorite: ev.priorite,
        echeance: ev.echeance,
        responsable: ev.responsable
      }
    ).subscribe({
      next: () => {
        this.savingEval.delete(ev.clause_code);
        this.cdr.markForCheck();
      },
      error: () => {
        this.savingEval.delete(ev.clause_code);
        this.cdr.markForCheck();
      }
    });
  }

  onStatutChange(ev: AuditEvaluation): void {
    // Auto-save dès que le statut change
    this.saveEvaluation(ev);
    // Rafraîchir les KPI si on est sur cet onglet
    if (this.activeTab === 'kpi-audit' && this.kpiAudit) {
      const t = setTimeout(() => this.loadKpiAudit(), 500);
      this.timers.push(t);
    }
  }

  // ── KPI Audit ─────────────────────────────────────────────────
  loadKpiAudit(): void {
    if (!this.selectedSession) return;
    this.loadingKpiAudit = true;
    this.destroyAuditCharts();
    this.cdr.markForCheck();

    this.auditSvc.getKpi(this.selectedSession.id).subscribe({
      next: (k) => {
        this.kpiAudit = k;
        this.loadingKpiAudit = false;
        this.cdr.markForCheck();
        this.zone.runOutsideAngular(() => {
          const t = setTimeout(() => {
            this.initAuditCharts();
            this.zone.run(() => this.cdr.markForCheck());
          }, 100);
          this.timers.push(t);
        });
      },
      error: () => { this.loadingKpiAudit = false; this.cdr.markForCheck(); }
    });
  }

  finaliserSession(): void {
    if (!this.selectedSession || !confirm('Finaliser cette session ?')) return;
    this.auditSvc.finaliser(this.selectedSession.id).subscribe({
      next: () => {
        if (this.selectedSession) this.selectedSession.statut = 'finalise';
        this.showSuccess('Session finalisée');
        this.cdr.markForCheck();
      }
    });
  }

  // ── Charts Audit ──────────────────────────────────────────────
  private initAuditCharts(): void {
    const k = this.kpiAudit;
    if (!k || k.total_clauses_evaluees === 0) return;

    this.makeAuditDoughnut(k);
    this.makeAuditRadar(k);
  }

  private makeAuditDoughnut(k: AuditKpi): void {
    const el = this.getCanvas(this.chartIds.auditDoughnut);
    if (!el) return;
    this.destroyChart(this.chartIds.auditDoughnut);

    const data = [
      k.nb_conforme, k.nb_non_conforme, k.nb_partiel,
      k.nb_planifie, k.nb_en_cours, k.nb_non_applicable
    ];
    if (data.every(v => v === 0)) return;

    try {
      const chart = new Chart(el, {
        type: 'doughnut',
        data: {
          labels: ['Conforme', 'Non conforme', 'Partiel', 'Planifié', 'En cours', 'N/A'],
          datasets: [{
            data,
            backgroundColor: ['#10b981', '#ef4444', '#f59e0b', '#3b82f6', '#8b5cf6', '#9ca3af'],
            borderWidth: 2,
            borderColor: '#fff'
          }]
        },
        options: {
          animation: false,
          responsive: true,
          maintainAspectRatio: false,
          cutout: '65%',
          plugins: {
            legend: { position: 'bottom', labels: { boxWidth: 10, font: { size: 11 } } },
            tooltip: { callbacks: { label: ctx => ` ${ctx.label}: ${ctx.parsed}` } }
          }
        }
      });
      this.charts.set(this.chartIds.auditDoughnut, chart);
    } catch (e) { console.warn('Audit doughnut:', e); }
  }

  private makeAuditRadar(k: AuditKpi): void {
    const el = this.getCanvas(this.chartIds.auditRadar);
    if (!el || !k.par_clause?.length) return;
    this.destroyChart(this.chartIds.auditRadar);

    const labels = k.par_clause.map(c => `Cl. ${c.clause_principale}`);
    const data = k.par_clause.map(c => c.taux_clause);
    if (data.every(v => v === 0)) return;

    try {
      const chart = new Chart(el, {
        type: 'radar',
        data: {
          labels,
          datasets: [{
            label: 'Taux de conformité (%)',
            data,
            backgroundColor: 'rgba(16,185,129,0.2)',
            borderColor: '#10b981',
            pointBackgroundColor: '#10b981',
            pointRadius: 5,
            borderWidth: 2
          }]
        },
        options: {
          animation: false,
          responsive: true,
          maintainAspectRatio: false,
          scales: {
            r: {
              min: 0, max: 100,
              ticks: { stepSize: 25, callback: v => v + '%', font: { size: 10 } },
              pointLabels: { font: { size: 12, weight: 600 }, color: '#374151' },
              grid: { color: 'rgba(0,0,0,0.07)' }
            }
          },
          plugins: {
            legend: { display: false },
            tooltip: { callbacks: { label: ctx => ` ${(ctx.parsed.r as number).toFixed(1)}%` } }
          }
        }
      });
      this.charts.set(this.chartIds.auditRadar, chart);
    } catch (e) { console.warn('Audit radar:', e); }
  }

  // ── Charts RSSI ───────────────────────────────────────────────
private initRssiCharts(): void {
  if (!this.kpiRssi?.has_data) return;

  // ── SOA radar ────────────────────────────────────────────────────
  const soa = this.kpiRssi.soa;
  if (soa?.has_data && soa.par_annexe?.length) {
    const el = this.getCanvas(this.chartIds.rssiSoaRadar);
    if (el) {
      this.destroyChart(this.chartIds.rssiSoaRadar);
      try {
        const c = new Chart(el, {
          type: 'radar',
          data: {
            labels: soa.par_annexe.map(a => a.annexe),
            datasets: [{
              label: 'SOA (%)',
              data: soa.par_annexe.map(a => a.taux_annexe),
              backgroundColor: 'rgba(59,130,246,0.2)',
              borderColor: '#3b82f6',
              pointBackgroundColor: '#3b82f6',
              pointRadius: 4, borderWidth: 2
            }]
          },
          options: {
            animation: false, responsive: true, maintainAspectRatio: false,
            scales: {
              r: {
                min: 0, max: 100,
                ticks: { stepSize: 25, callback: (v: any) => v + '%', font: { size: 10 } },
                pointLabels: { font: { size: 11 } },
                grid: { color: 'rgba(0,0,0,0.07)' }
              }
            },
            plugins: {
              legend: { display: false },
              tooltip: { callbacks: { label: (ctx: any) => `${(ctx.parsed.r as number).toFixed(1)}%` } }
            }
          }
        });
        this.charts.set(this.chartIds.rssiSoaRadar, c);
      } catch (e) { console.warn('RSSI SOA radar:', e); }
    }
  }

  // ── Publication bar chart ─────────────────────────────────────────
  const pub = this.kpiRssi.publications;   // ← publication (pas publications)
  if (pub?.has_data && pub.par_publication?.length) {
    const el = this.getCanvas(this.chartIds.rssiPubBar);
    if (el) {
      this.destroyChart(this.chartIds.rssiPubBar);
      this.makeRssiPubBar(el, pub.par_publication);
    }
  }

  // ── Formation bar chart ───────────────────────────────────────────
  const form = this.kpiRssi.formation;
  if (form?.has_data && form.par_session?.length) {
    const el = this.getCanvas(this.chartIds.rssiFormBar);
    if (el) {
      this.destroyChart(this.chartIds.rssiFormBar);
      this.makeRssiFormBar(el, form.par_session);
    }
  }
}

private makeRssiPubBar(
  el: HTMLCanvasElement,
  pubs: { titre: string; taux_lecture: number; priorite: string }[]
): void {
  const colorMap: Record<string, string> = {
    urgente: '#ef4444', haute: '#f59e0b',
    normale: '#3b82f6', basse:  '#9ca3af'
  };
  try {
    const c = new Chart(el, {
      type: 'bar',
      data: {
        labels: pubs.map(p => p.titre.length > 20 ? p.titre.slice(0, 18) + '…' : p.titre),
        datasets: [{
          label: 'Taux de lecture (%)',
          data: pubs.map(p => Math.min(p.taux_lecture, 100)),
          backgroundColor: pubs.map(p => colorMap[p.priorite] ?? '#6b7280'),
          borderRadius: 5,
          borderSkipped: false
        }]
      },
      options: {
        animation: false, responsive: true, maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: { callbacks: {
            title: (items: any) => pubs[items[0].dataIndex].titre,
            label: (ctx: any) => ` ${(ctx.parsed.y as number).toFixed(1)}%`
          }}
        },
        scales: {
          y: { min: 0, max: 100, ticks: { stepSize: 25, callback: (v: any) => v + '%', font: { size: 10 } }, grid: { color: 'rgba(0,0,0,0.04)' } },
          x: { grid: { display: false }, ticks: { font: { size: 10 }, maxRotation: 35, minRotation: 35 } }
        }
      }
    });
    this.charts.set(this.chartIds.rssiPubBar, c);
  } catch (e) { console.warn('RSSI pub bar:', e); }
}

private makeRssiFormBar(
  el: HTMLCanvasElement,
  sessions: { titre: string; statut: string; taux_participation: number; nb_presents: number; max_participants: number | null; nb_inscrits: number }[]
): void {
  const colorMap: Record<string, string> = {
    planifie: '#3b82f6', en_cours: '#f59e0b',
    termine:  '#10b981', annule:   '#ef4444'
  };
  try {
    const c = new Chart(el, {
      type: 'bar',
      data: {
        labels: sessions.map(s => s.titre.length > 20 ? s.titre.slice(0, 18) + '…' : s.titre),
        datasets: [{
          label: 'Taux de participation (%)',
          data: sessions.map(s => Math.min(s.taux_participation, 100)),
          backgroundColor: sessions.map(s => colorMap[s.statut] ?? '#6b7280'),
          borderRadius: 5,
          borderSkipped: false
        }]
      },
      options: {
        animation: false, responsive: true, maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: { callbacks: {
            title: (items: any) => sessions[items[0].dataIndex].titre,
            label: (ctx: any) => {
              const s = sessions[ctx.dataIndex];
              const denom = s.max_participants ?? s.nb_inscrits;
              return [
                ` Participation : ${(ctx.parsed.y as number).toFixed(1)}%`,
                ` Présents : ${s.nb_presents} / ${denom}`
              ];
            }
          }}
        },
        scales: {
          y: { min: 0, max: 100, ticks: { stepSize: 25, callback: (v: any) => v + '%', font: { size: 10 } }, grid: { color: 'rgba(0,0,0,0.04)' } },
          x: { grid: { display: false }, ticks: { font: { size: 10 }, maxRotation: 35, minRotation: 35 } }
        }
      }
    });
    this.charts.set(this.chartIds.rssiFormBar, c);
  } catch (e) { console.warn('RSSI form bar:', e); }
}
  // ── Getters liste filtrée ─────────────────────────────────────
  get evaluationsFiltrees(): AuditEvaluation[] {
    return this.evaluations.filter(ev => {
      const ms = !this.filtreStatutEval || ev.statut === this.filtreStatutEval;
      const mc = !this.filtreClause || ev.clause_code.startsWith(this.filtreClause);
      return ms && mc;
    });
  }

  get clausesPrincipales(): string[] {
    const codes = [...new Set(this.evaluations.map(e => e.clause_code.split('.')[0]))];
    return codes.sort();
  }

  // ── Helpers ───────────────────────────────────────────────────
  getStatutColor(s: string): string {
    return this.STATUTS.find(x => x.value === s)?.color ?? '#9ca3af';
  }
  getStatutLabel(s: string): string {
    return this.STATUTS.find(x => x.value === s)?.label ?? s;
  }
  labelType(t: string): string {
    return ({ information: 'Info', alerte: 'Alerte', procedure: 'Procédure', politique: 'Politique', note: 'Note' } as Record<string, string>)[t] ?? t;
  }
  isParent(ev: AuditEvaluation): boolean { return !ev.parent_code; }
  isSaving(code: string): boolean { return this.savingEval.has(code); }

  getProgressColor(v: number): string {
    return v >= 80 ? '#10b981' : v >= 50 ? '#f59e0b' : '#ef4444';
  }

  private destroyAuditCharts(): void {
    [this.chartIds.auditRadar, this.chartIds.auditDoughnut].forEach(id => this.destroyChart(id));
  }

  private destroyAll(): void {
    Object.values(this.chartIds).forEach(id => this.destroyChart(id));
  }

  private destroyChart(id: string): void {
    const c = this.charts.get(id);
    if (c) { try { c.destroy(); } catch (_) { } this.charts.delete(id); }
    const el = document.getElementById(id) as HTMLCanvasElement | null;
    if (el) { const ex = Chart.getChart(el); if (ex) { try { ex.destroy(); } catch (_) { } } }
  }

  private getCanvas(id: string): HTMLCanvasElement | null {
    const el = document.getElementById(id);
    return el instanceof HTMLCanvasElement ? el : null;
  }

  private showSuccess(msg: string): void {
    this.success = msg;
    const t = setTimeout(() => { this.success = ''; this.cdr.markForCheck(); }, 3000);
    this.timers.push(t);
  }
}
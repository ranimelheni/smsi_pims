import { Component, OnInit } from '@angular/core';
import { Router, NavigationEnd, RouterOutlet, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { User } from '../../models/models';
import { filter } from 'rxjs/operators';

interface NavItem {
  label:    string;
  icon:     string;
  route:    string;
  clause?:  string;
  disabled?: boolean;
  roles:    string[];  // ← rôles autorisés
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
  currentUser: User | null = null;
  sidebarCollapsed = false;
  activeRoute      = '';
  showUserMenu     = false;
isRiskAnalysisEnabled = false;
  // Toutes les entrées de nav avec les rôles autorisés
  allNavItems: NavItem[] = [
    {
      label:  'Organismes',
      icon:   'building',
      route:  '/dashboard/organisms',
      clause: 'Administration',
      roles:  ['super_admin', 'admin_organism']
    },
   
 {
  label:'fiche processus',
  icon: 'process',
  route: '/dashboard/fiche-processus',
  roles: ['pilote_processus']
},
    {
  label:  'Protection des données',
  icon:   'shield',
  route:  '/dpo',
  clause: 'DPO',
  roles:  ['dpo']
},
{
  label: 'Fiche technique',
  icon:  'tool',
  route: '/dashboard/fiche-technique',
  roles: ['membre_equipe_technique']
},
{
  label: 'Contexte de l\'organisme',
  icon:  'shield',
  route: '/dashboard/rssi',
  roles: ['rssi']
},
{
  label: 'Engagement de la Direction ',
  icon:  'briefcase',
  route: '/dashboard/direction',
  roles: ['direction', 'comite_securite']
},
{
  label: 'Planification',
  icon:  'target',
  route: '/dashboard/clause6',
  roles: ['rssi']
},
{
  label: 'Planification',
  icon: 'target',
  route: '/dashboard/direction/clause6',
  roles: ['direction', 'comite_securite']
},
{
  label: 'Analyse de risque',
  icon: 'risk',
  route: '/dashboard/ebios',
  roles: ['rssi'],
  disabled: true // sera dynamique
},
{
  label: 'Registre des traitements',
  icon: 'database',
  route: '/dashboard/dpo/registre',  
  roles: ['dpo']
},
{
  label:  'Support',
  icon:   'support',
  route:  '/dashboard/clause7',
  roles:  [
    'rssi',
    'admin_organism',
    'direction',
    'employe',
    'pilote_processus',
    'dpo'
  ]
},
{
  label:  'KPIs',
  icon:   'chart',
  route:  '/dashboard/kpi-dashboard',
  roles:  ['rssi', 'direction']
},
// Ajouter dans allNavItems
{
  label: 'Tableau de bord Auditeur',
  icon:  'audit',
  route: '/dashboard/auditeur-dashboard',
  roles: ['auditeur']
},
{
  label: 'Suivi des non-conformités',
  icon:  'nc',
  route: '/dashboard/suivi-nc',
  roles: ['rssi', 'direction']
}

  ];

  constructor(private auth: AuthService, private router: Router) {}

  ngOnInit(): void {
    this.currentUser = this.auth.getCurrentUser();
    this.checkRiskAccess(); 
    this.router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe((e: any) => { this.activeRoute = e.url; });
    this.activeRoute = this.router.url;
  }
checkRiskAccess() {


  this.isRiskAnalysisEnabled = true; // ⚠️ test temporaire
}
  // Items visibles selon le rôle
  get navItems(): NavItem[] {
  const role = this.currentUser?.role || '';

  return this.allNavItems
    .map(item => {
      if (item.label === 'Analyse de risque') {
        return {
          ...item,
          disabled: !this.isRiskAnalysisEnabled
        };
      }
      return item;
    })
    .filter(item => item.roles.includes(role));
}

  get userInitials(): string {
    if (!this.currentUser) return 'AD';
    return (this.currentUser.prenom[0] + this.currentUser.nom[0]).toUpperCase();
  }

  get roleLabel(): string {
    const labels: Record<string, string> = {
      super_admin:    'Super Administrateur',
      admin_organism: 'Admin Organisme',
      rssi:           'RSSI'
    };
    return labels[this.currentUser?.role || ''] || this.currentUser?.role || '';
  }

  navigate(item: NavItem): void {
    if (item.disabled || !item.route) return;
    this.router.navigate([item.route]);
  }

  isActive(route: string): boolean { return route ? this.activeRoute.startsWith(route) : false; }
  toggleSidebar():  void { this.sidebarCollapsed = !this.sidebarCollapsed; }
  toggleUserMenu(): void { this.showUserMenu = !this.showUserMenu; }
  logout():         void { this.auth.logout(); }
}
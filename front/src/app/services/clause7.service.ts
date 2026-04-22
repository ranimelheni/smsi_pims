import { Injectable }    from '@angular/core';
import { HttpClient }    from '@angular/common/http';
import { Observable }    from 'rxjs';

@Injectable({ providedIn: 'root' })
export class Clause7Service {
  private api = 'http://localhost:8080/api/clause7';

  constructor(private http: HttpClient) {}

  // ── Profil ───────────────────────────────────────────────────────
  getMonProfil():                       Observable<any>    { return this.http.get(`${this.api}/profil/me`); }
  updateMonProfil(d: any):              Observable<any>    { return this.http.put(`${this.api}/profil/me`, d); }
  getProfilsOrganism():                 Observable<any[]>  { return this.http.get<any[]>(`${this.api}/profils/organism`); }
  getProfilById(id: number):            Observable<any>    { return this.http.get(`${this.api}/profils/${id}`); }
  evaluerEmploye(id: number, d: any):   Observable<any>    { return this.http.put(`${this.api}/profils/${id}/evaluer`, d); }

  // ── CV ───────────────────────────────────────────────────────────
  uploadCv(file: File): Observable<any> {
    const fd = new FormData();
    fd.append('fichier', file, file.name);
    return this.http.post(`${this.api}/profil/me/cv`, fd);
  }
  supprimerCv(): Observable<any> { return this.http.delete(`${this.api}/profil/me/cv`); }

  // ── Certifications ───────────────────────────────────────────────
  ajouterCertification(data: any, file?: File): Observable<any> {
    const fd = new FormData();
    fd.append('nom', data.nom);
    if (data.organisme)       fd.append('organisme',       data.organisme);
    if (data.date_obtention)  fd.append('date_obtention',  data.date_obtention);
    if (data.date_expiration) fd.append('date_expiration', data.date_expiration);
    if (file)                 fd.append('fichier',         file, file.name);
    return this.http.post(`${this.api}/profil/me/certifications`, fd);
  }
  supprimerCertification(id: number): Observable<any> {
    return this.http.delete(`${this.api}/profil/me/certifications/${id}`);
  }

  // ── Téléchargements ──────────────────────────────────────────────
  downloadCv(userId: number): Observable<Blob> {
    return this.http.get(`${this.api}/download/cv/${userId}`,
      { responseType: 'blob' });
  }
  downloadCertif(certifId: number): Observable<Blob> {
    return this.http.get(`${this.api}/download/certif/${certifId}`,
      { responseType: 'blob' });
  }
  downloadDocument(docId: number): Observable<Blob> {
    return this.http.get(`${this.api}/download/document/${docId}`,
      { responseType: 'blob' });
  }

  // ── Formations ───────────────────────────────────────────────────
  getSessions():                         Observable<any[]> { return this.http.get<any[]>(`${this.api}/formations`); }
  creerSession(d: any):                  Observable<any>   { return this.http.post(`${this.api}/formations`, d); }
  sInscrire(id: number):                 Observable<any>   { return this.http.post(`${this.api}/formations/${id}/inscrire`, {}); }
  seDesinscrire(id: number):             Observable<any>   { return this.http.delete(`${this.api}/formations/${id}/desinscrire`); }
  getParticipants(id: number):           Observable<any[]> { return this.http.get<any[]>(`${this.api}/formations/${id}/participants`); }
  evaluerPresence(id: number, d: any):   Observable<any>   { return this.http.put(`${this.api}/formations/participation/${id}/evaluer`, d); }

  // ── Communications ───────────────────────────────────────────────
  getPublications():                     Observable<any[]> { return this.http.get<any[]>(`${this.api}/communications`); }
  creerPublication(d: any):              Observable<any>   { return this.http.post(`${this.api}/communications`, d); }
  publier(id: number):                   Observable<any>   { return this.http.patch(`${this.api}/communications/${id}/publier`, {}); }
  marquerLu(id: number):                 Observable<any>   { return this.http.post(`${this.api}/communications/${id}/lire`, {}); }

  // ── Documents ────────────────────────────────────────────────────
  getDocuments():                        Observable<any[]> { return this.http.get<any[]>(`${this.api}/documents`); }
  getDocumentsFiche(ficheId: number):    Observable<any[]> { return this.http.get<any[]>(`${this.api}/documents/fiche/${ficheId}`); }
  approuverDocument(id: number):         Observable<any>   { return this.http.patch(`${this.api}/documents/${id}/approuver`, {}); }

  creerDocument(data: any, file?: File): Observable<any> {
    const fd = new FormData();
    fd.append('titre', data.titre);
    if (data.reference)       fd.append('reference',       data.reference);
    if (data.type_document)   fd.append('type_document',   data.type_document);
    if (data.version)         fd.append('version',         data.version);
    if (data.description)     fd.append('description',     data.description);
    if (data.fiche_id)        fd.append('fiche_id',        String(data.fiche_id));
    if (data.date_revision)   fd.append('date_revision',   data.date_revision);
    if (data.date_expiration) fd.append('date_expiration', data.date_expiration);
    if (file)                 fd.append('fichier',         file, file.name);
    return this.http.post(`${this.api}/documents`, fd);
  }
}
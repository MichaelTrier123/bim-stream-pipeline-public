import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { environment } from '../environments/environment';
import { Observable, catchError, filter, map, of, switchMap, take, takeWhile, throwError, timeout, timer } from 'rxjs';
import { ImportLookupResponse, PresignResponse } from './api-types';

export interface UploadResult {
  key: string;
}

@Injectable({ providedIn: 'root' })
export class UploadService {
  private readonly baseUrl = environment.apiBaseUrl;

  constructor(private http: HttpClient) {}

  presign(): Observable<PresignResponse> {
    return this.http.post<PresignResponse>(`${this.baseUrl}/uploads/presign`, {});
  }

  uploadIfc(file: File): Observable<UploadResult> {
    // VIGTIGT: din presign signer content-type;host, så vi SKAL sende Content-Type.
    const contentType = file.type || 'application/octet-stream';

    return this.presign().pipe(
      switchMap((p) => {
        const headers = new HttpHeaders({ 'Content-Type': contentType });

        // HttpClient kan godt PUT’e til S3 presigned URL
        return this.http.put(p.uploadUrl, file, { headers, responseType: 'text' }).pipe(
          map(() => ({ key: p.key }))
        );
      })
    );
  }

  lookupByKey(key: string) {
    return this.http.get<ImportLookupResponse>(`${this.baseUrl}/imports/by-key`, {
      params: { key }
    });
  }
  
  pollImportByKey(key: string, intervalMs = 2000, timeoutMs = 60000) {
    return timer(0, intervalMs).pipe(
      switchMap(() =>
        this.lookupByKey(key).pipe(
          // Hvis vi får 200, mapper vi til et “done” signal
          map((res: ImportLookupResponse) => ({ done: true, res })),
          // Hvis 404, så er den bare ikke klar endnu -> done: false
          catchError((err: any) => {
            if (err?.status === 404) return of({ done: false as const });
            return throwError(() => err);
          })
        )
      ),
      // fortsæt indtil done
      filter((x: any) => x.done === true),
      take(1),
      map((x: any) => x.res as ImportLookupResponse),
      timeout(timeoutMs)
    );
  }
  
}

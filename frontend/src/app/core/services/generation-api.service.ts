import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { GenerationOptions } from '../models/generation-options.model';

@Injectable({
  providedIn: 'root'
})
export class GenerationApiService {
  constructor(private http: HttpClient) {}

  /**
   * Triggers the synchronous zip generation pipeline and initiates browser download of the generated ZIP file.
   * Leverages custom RxJS tap operators to decouple browser download logic safely from presentation handlers.
   * 
   * @param options Code generation parameters and schema setup.
   * @returns Observable emitting the raw HTTP Response holding the Blob.
   */
  downloadProject(options: GenerationOptions): Observable<HttpResponse<Blob>> {
    return this.http.post('/api/v1/generate', options, {
      responseType: 'blob',
      observe: 'response'
    }).pipe(
      tap((response: HttpResponse<Blob>) => {
        const blob = response.body;
        if (blob) {
          // Create temp browser object URL pointing to the byte buffer
          const url = window.URL.createObjectURL(blob);
          
          // Instantiate a virtual DOM anchor element to trigger download action
          const a = document.createElement('a');
          a.style.display = 'none';
          a.href = url;
          a.download = `api-forge-${options.packageName}.zip`;
          
          document.body.appendChild(a);
          a.click();
          
          // Perform defensive cleanups to avoid resource leaks in client memory
          document.body.removeChild(a);
          window.URL.revokeObjectURL(url);
        }
      })
    );
  }
}

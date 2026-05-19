import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { GenerationOptions } from '../models/generation-options.model';
import {
  SseProgressEvent,
  SseFileEvent,
  SseCompleteEvent,
  SseErrorEvent
} from '../models/generation-events.model';

@Injectable({
  providedIn: 'root'
})
export class SseGenerationService {
  private eventSource: EventSource | null = null;

  private progressSubject = new Subject<SseProgressEvent>();
  private fileSubject = new Subject<SseFileEvent>();
  private completeSubject = new Subject<SseCompleteEvent>();
  private errorSubject = new Subject<SseErrorEvent>();

  // Expose as read-only Observables to protect stream encapsulation
  progress$: Observable<SseProgressEvent> = this.progressSubject.asObservable();
  file$: Observable<SseFileEvent> = this.fileSubject.asObservable();
  complete$: Observable<SseCompleteEvent> = this.completeSubject.asObservable();
  error$: Observable<SseErrorEvent> = this.errorSubject.asObservable();

  /**
   * Initializes native EventSource stream connection to preview code generation in real-time.
   * Cleans up any previously active sessions defensively.
   * 
   * @param options Target package, options, and SQL schemas selection parameters.
   */
  connect(options: GenerationOptions): void {
    // Defensively terminate any hanging connection first
    this.disconnect();

    // Map generation options to query string parameters using native URLSearchParams
    const queryParams = new URLSearchParams({
      sql: options.sql,
      packageName: options.packageName,
      generateJwt: String(options.generateJwt),
      generatePagination: String(options.generatePagination),
      generateSoftDelete: String(options.generateSoftDelete),
      enrichWithLlm: String(options.enrichWithLlm)
    });

    const sseUrl = `/api/v1/generate/preview?${queryParams.toString()}`;
    this.eventSource = new EventSource(sseUrl);

    // 1. Progress Step Event Callback
    this.eventSource.addEventListener('progress', (event: MessageEvent) => {
      try {
        const data: SseProgressEvent = JSON.parse(event.data);
        this.progressSubject.next(data);
      } catch (err) {
        this.errorSubject.next({ message: 'Failed to parse progress event stream payload.' });
      }
    });

    // 2. File Generation Event Callback
    this.eventSource.addEventListener('file', (event: MessageEvent) => {
      try {
        const data: SseFileEvent = JSON.parse(event.data);
        this.fileSubject.next(data);
      } catch (err) {
        this.errorSubject.next({ message: 'Failed to parse generated file event stream payload.' });
      }
    });

    // 3. Generation Complete Event Callback
    this.eventSource.addEventListener('complete', (event: MessageEvent) => {
      try {
        const data: SseCompleteEvent = JSON.parse(event.data);
        this.completeSubject.next(data);
      } catch (err) {
        this.completeSubject.next({ message: 'Generation complete.', fileCount: 0 });
      } finally {
        // Automatically close connections to prevent leaks
        this.disconnect();
      }
    });

    // 4. Exception Event Callback
    this.eventSource.addEventListener('error', (event: MessageEvent) => {
      try {
        const data: SseErrorEvent = JSON.parse(event.data);
        this.errorSubject.next(data);
      } catch (err) {
        this.errorSubject.next({ message: 'An internal error occurred during generation.' });
      } finally {
        // Automatically close connections on failure
        this.disconnect();
      }
    });

    // 5. Standard EventSource Connection Error callback
    this.eventSource.onerror = () => {
      // Direct EventSource connection issues (timeouts, network, etc.)
      this.errorSubject.next({ message: 'Server connection lost or preview stream timed out.' });
      this.disconnect();
    };
  }

  /**
   * Terminate and clean up EventSource connection.
   * Highly defensive checking protects from null references or premature closures.
   */
  disconnect(): void {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
  }
}

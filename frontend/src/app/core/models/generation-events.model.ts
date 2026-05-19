export interface SseProgressEvent {
  step: string;
  message: string;
}

export interface SseFileEvent {
  path: string;
  content: string;
}

export interface SseCompleteEvent {
  message: string;
  fileCount: number;
}

export interface SseErrorEvent {
  message: string;
}

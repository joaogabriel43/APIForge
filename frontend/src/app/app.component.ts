import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { MonacoEditorModule } from 'ngx-monaco-editor-v2';

interface GeneratedFile {
  name: string;
  content: string;
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule, MonacoEditorModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  title = 'APIForge Code Playground';

  // Input bindings
  sqlCode = `CREATE TABLE users (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE posts (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    user_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);`;

  packageName = 'com.apiforge.generated';
  generateJwt = false;
  generatePagination = false;
  generateSoftDelete = false;
  enrichWithLlm = false;

  // UI State
  isGenerating = false;
  generationError = '';
  logs: string[] = [];
  files: GeneratedFile[] = [];
  selectedFile: GeneratedFile | null = null;
  selectedFileContent = '';

  // Monaco Editor Options
  sqlEditorOptions = {
    theme: 'vs-dark',
    language: 'sql',
    fontSize: 14,
    minimap: { enabled: false },
    automaticLayout: true,
    padding: { top: 10 }
  };

  javaEditorOptions = {
    theme: 'vs-dark',
    language: 'java',
    fontSize: 14,
    minimap: { enabled: true },
    readOnly: true,
    automaticLayout: true,
    padding: { top: 10 }
  };

  constructor(private http: HttpClient) {}

  // Run live SSE preview generation
  runLivePreview(): void {
    if (!this.sqlCode.trim() || !this.packageName.trim()) {
      this.generationError = 'SQL Schema and Package Name are required.';
      return;
    }

    this.isGenerating = true;
    this.generationError = '';
    this.logs = ['Initiating real-time preview pipeline...'];
    this.files = [];
    this.selectedFile = null;

    const params = new URLSearchParams({
      sql: this.sqlCode,
      packageName: this.packageName,
      generateJwt: String(this.generateJwt),
      generatePagination: String(this.generatePagination),
      generateSoftDelete: String(this.generateSoftDelete),
      enrichWithLlm: String(this.enrichWithLlm)
    });

    const sseUrl = `/api/v1/generate/preview?${params.toString()}`;
    const eventSource = new EventSource(sseUrl);

    eventSource.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        if (data.type === 'parsing') {
          this.logs.push(`[Parser] ${data.message}`);
        } else if (data.type === 'generating') {
          this.logs.push(`[Generator] Rendered ${data.filePath}`);
          
          // Add or update file in list
          const existingIdx = this.files.findIndex(f => f.name === data.filePath);
          const updatedFile = { name: data.filePath, content: data.content };
          if (existingIdx !== -1) {
            this.files[existingIdx] = updatedFile;
          } else {
            this.files.push(updatedFile);
          }

          // Auto-select first generated file
          if (!this.selectedFile) {
            this.selectFile(updatedFile);
          } else if (this.selectedFile.name === data.filePath) {
            this.selectFile(updatedFile);
          }
        }
      } catch (err) {
        // Fallback for non-JSON lines or done events
        if (event.data === 'done' || event.data.includes('complete')) {
          this.logs.push('[Pipeline] Generation finished successfully.');
          eventSource.close();
          this.isGenerating = false;
        } else {
          this.logs.push(`[Info] ${event.data}`);
        }
      }
    };

    eventSource.addEventListener('done', () => {
      this.logs.push('[Pipeline] Live generation completed successfully.');
      eventSource.close();
      this.isGenerating = false;
    });

    eventSource.onerror = (err) => {
      console.error('SSE Error:', err);
      // Wait a frame or check if already closed
      if (eventSource.readyState === EventSource.CLOSED) {
        return;
      }
      this.logs.push('[Error] Connection interrupted or pipeline failed.');
      this.generationError = 'An error occurred during real-time generation.';
      eventSource.close();
      this.isGenerating = false;
    };
  }

  // Download ZIP output
  downloadZip(): void {
    if (!this.sqlCode.trim() || !this.packageName.trim()) {
      this.generationError = 'SQL Schema and Package Name are required.';
      return;
    }

    this.isGenerating = true;
    this.generationError = '';
    this.logs = ['Packaging project source code ZIP...'];

    const payload = {
      sql: this.sqlCode,
      packageName: this.packageName,
      generateJwt: this.generateJwt,
      generatePagination: this.generatePagination,
      generateSoftDelete: this.generateSoftDelete,
      enrichWithLlm: this.enrichWithLlm
    };

    this.http.post('/api/v1/generate', payload, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        this.logs.push('[Success] Download initiated.');
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `${this.packageName.split('.').pop() || 'apiforge'}-project.zip`;
        a.click();
        window.URL.revokeObjectURL(url);
        this.isGenerating = false;
      },
      error: (err) => {
        console.error(err);
        this.generationError = 'Failed to generate ZIP project. Check backend logs.';
        this.logs.push('[Error] Packaging failed.');
        this.isGenerating = false;
      }
    });
  }

  selectFile(file: GeneratedFile): void {
    this.selectedFile = file;
    this.selectedFileContent = file.content;
  }

  getFileName(path: string): string {
    if (!path) return '';
    return path.substring(path.lastIndexOf('/') + 1);
  }

  isFileSelected(file: GeneratedFile): boolean {
    return this.selectedFile !== null && this.selectedFile.name === file.name;
  }
}

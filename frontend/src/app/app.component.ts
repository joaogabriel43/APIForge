import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MonacoEditorModule } from 'ngx-monaco-editor-v2';
import { Subscription } from 'rxjs';
import { GenerationApiService } from './core/services/generation-api.service';
import { SseGenerationService } from './core/services/sse-generation.service';
import { GenerationOptions } from './core/models/generation-options.model';

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
  private previewSubs: Subscription[] = [];

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

  constructor(
    private generationApiService: GenerationApiService,
    private sseGenerationService: SseGenerationService
  ) {}

  // Run live SSE preview generation
  runLivePreview(): void {
    if (!this.sqlCode.trim() || !this.packageName.trim()) {
      this.generationError = 'SQL Schema and Package Name are required.';
      return;
    }

    // Clear any previous active subscriptions to protect memory
    this.previewSubs.forEach(s => s.unsubscribe());
    this.previewSubs = [];

    this.isGenerating = true;
    this.generationError = '';
    this.logs = ['Initiating real-time preview pipeline...'];
    this.files = [];
    this.selectedFile = null;
    this.selectedFileContent = '';

    const options: GenerationOptions = {
      sql: this.sqlCode,
      packageName: this.packageName,
      generateJwt: this.generateJwt,
      generatePagination: this.generatePagination,
      generateSoftDelete: this.generateSoftDelete,
      enrichWithLlm: this.enrichWithLlm
    };

    // Subscriptions setup
    const progressSub = this.sseGenerationService.progress$.subscribe(data => {
      this.logs.push(`[Parser] ${data.message}`);
    });

    const fileSub = this.sseGenerationService.file$.subscribe(data => {
      this.logs.push(`[Generator] Rendered ${data.path}`);
      
      const updatedFile = { name: data.path, content: data.content };
      const existingIdx = this.files.findIndex(f => f.name === data.path);
      if (existingIdx !== -1) {
        this.files[existingIdx] = updatedFile;
      } else {
        this.files.push(updatedFile);
      }

      if (!this.selectedFile) {
        this.selectFile(updatedFile);
      } else if (this.selectedFile.name === data.path) {
        this.selectFile(updatedFile);
      }
    });

    const completeSub = this.sseGenerationService.complete$.subscribe(data => {
      this.logs.push(`[Success] ${data.message} (${data.fileCount} files created)`);
      this.isGenerating = false;
      this.previewSubs.forEach(s => s.unsubscribe());
    });

    const errorSub = this.sseGenerationService.error$.subscribe(data => {
      this.logs.push(`[Error] ${data.message}`);
      this.generationError = data.message;
      this.isGenerating = false;
      this.previewSubs.forEach(s => s.unsubscribe());
    });

    this.previewSubs.push(progressSub, fileSub, completeSub, errorSub);

    // Trigger connection
    this.sseGenerationService.connect(options);
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

    const options: GenerationOptions = {
      sql: this.sqlCode,
      packageName: this.packageName,
      generateJwt: this.generateJwt,
      generatePagination: this.generatePagination,
      generateSoftDelete: this.generateSoftDelete,
      enrichWithLlm: this.enrichWithLlm
    };

    this.generationApiService.downloadProject(options).subscribe({
      next: () => {
        this.logs.push('[Success] Download initiated.');
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

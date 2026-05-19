import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
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
  imports: [CommonModule, ReactiveFormsModule, MonacoEditorModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
  title = 'APIForge Code Playground';
  generatorForm!: FormGroup;

  // Initial SQL input schema config
  initialSql = `CREATE TABLE users (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE posts (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    body TEXT,
    user_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);`;

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
    private fb: FormBuilder,
    private generationApiService: GenerationApiService,
    private sseGenerationService: SseGenerationService
  ) {}

  ngOnInit(): void {
    this.initForm();
  }

  private initForm(): void {
    this.generatorForm = this.fb.group({
      sql: [this.initialSql, Validators.required],
      packageName: ['com.apiforge.generated', [
        Validators.required,
        Validators.pattern(/^[a-z]+(\.[a-z]+)*$/)
      ]],
      generateJwt: [false],
      generatePagination: [true],
      generateSoftDelete: [false],
      enrichWithLlm: [false]
    });
  }

  // Reactive validation convenience helper getter
  get f() {
    return this.generatorForm.controls;
  }

  // Run live SSE preview generation
  runLivePreview(): void {
    if (this.generatorForm.invalid || this.isGenerating) {
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

    const options: GenerationOptions = this.generatorForm.value;

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
    if (this.generatorForm.invalid || this.isGenerating) {
      return;
    }

    this.isGenerating = true;
    this.generationError = '';
    this.logs = ['Packaging project source code ZIP...'];

    const options: GenerationOptions = this.generatorForm.value;

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

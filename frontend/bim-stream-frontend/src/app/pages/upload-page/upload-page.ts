import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UploadService } from '../../upload-service';
import { finalize, switchMap } from 'rxjs/operators';
import { ImportLookupResponse } from '../../api-types';
import { ImportViewComponent } from '../../components/import-view/import-view';

@Component({
  selector: 'app-upload-page',
  standalone: true,
  imports: [
    CommonModule,
    ImportViewComponent,
  ],
  templateUrl: './upload-page.html',
  styleUrl: './upload-page.scss',
})
export class UploadPage {
  selectedFile?: File;
  uploading = false;
  error?: string;
  lastKey?: string;
  processing = false;
  processingStatus?: string;
  importResult?: ImportLookupResponse;

  constructor(private upload: UploadService) {}

  get statusLabel(): string {
    if (this.error) return 'Error';
    if (this.uploading) return 'Uploading';
    if (this.processing) return 'Processing';
    if (this.importResult) return 'Done';
    return 'Idle';
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    this.selectedFile = file;
    this.error = undefined;
    this.lastKey = undefined;
  }

  onUpload(): void {
    if (!this.selectedFile) return;

    this.error = undefined;
    this.importResult = undefined;
    this.processingStatus = undefined;

    this.uploading = true;
    this.processing = true;
    this.processingStatus = 'Uploading...';

    this.upload.uploadIfc(this.selectedFile).pipe(
      finalize(() => (this.uploading = false)),
      switchMap((res) => {
        this.lastKey = res.key;
        this.processingStatus = 'Processing...';
        return this.upload.pollImportByKey(res.key);
      }),
      finalize(() => (this.processing = false))
    ).subscribe({
      next: (lookup) => {
        this.importResult = lookup;
        this.processingStatus = 'Done';
      },
      error: (e: unknown) => {
        this.processingStatus = undefined;
        this.error = e instanceof Error ? e.message : 'Upload/lookup failed';
        this.processing = false;
      }
    });
  }

}

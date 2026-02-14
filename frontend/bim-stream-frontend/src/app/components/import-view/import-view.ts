import { Component, Input, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ImportLookupResponse, ElementCountDto } from '../../api-types';


@Component({
  selector: 'app-import-view',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './import-view.html',
  styleUrl: './import-view.scss',
})
export class ImportViewComponent implements OnChanges {
  @Input() data?: ImportLookupResponse;

  displayedColumns: string[] = ['elementType', 'count'];
  tableData: ElementCountDto[] = [];

  ngOnChanges(): void {
    this.tableData = this.data?.import?.elementCounts ?? [];
  }
}

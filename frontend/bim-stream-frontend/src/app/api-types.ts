export interface PresignResponse {
  uploadUrl: string;
  key: string;
  expiresSeconds?: number;
  issuedAt?: string;
}

export interface ElementCountDto {
  elementType: string;
  count: number;
}

export interface ImportDto {
  id: number;
  createdAt: string;
  sourceBucket: string;
  sourceKey: string;
  ifcSchema: string | null;
  sourceApplication: string | null;
  projectName: string | null;
  siteName: string | null;
  buildingName: string | null;
  elementCounts: ElementCountDto[];
}

export interface ImportLookupResponse {
  key: string;
  import: ImportDto;
}

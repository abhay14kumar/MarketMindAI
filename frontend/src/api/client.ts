import {
  fallbackDocuments,
  fallbackDownloadJobs,
  fallbackSchedulerJobs,
  fallbackSourceHealth,
  fallbackSources,
} from './mockFallbacks';
import type {
  DocumentRecord,
  DownloadJob,
  PageResponse,
  PortfolioAllocation,
  PortfolioHolding,
  PortfolioImportJob,
  PortfolioImportResponse,
  PortfolioSummary,
  ManualPriceRequest,
  PriceFeedJob,
  PriceProviderStatus,
  PriceSnapshot,
  QueryPayload,
  SchedulerJob,
  Source,
  SourceHealth,
  SourceValidation,
  AiAnswer,
  AiAskRequest,
  DocumentChunk,
  EmbeddingJob,
} from './types';

const API_BASE_URL = (import.meta.env.VITE_BACKEND_URL ?? 'http://localhost:8080').replace(/\/$/, '');

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status?: number,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...init,
      headers: { Accept: 'application/json', ...init?.headers },
    });
  } catch (error) {
    throw new ApiError(error instanceof Error ? error.message : 'Backend is unavailable.');
  }

  if (!response.ok) {
    let detail = `Backend returned HTTP ${response.status}.`;
    try {
      const problem = await response.json() as { detail?: string };
      detail = problem.detail || detail;
    } catch {
      // Preserve the status-based message when the response is not JSON.
    }
    throw new ApiError(detail, response.status);
  }
  return response.json() as Promise<T>;
}

function isBackendUnavailable(error: unknown) {
  return error instanceof ApiError
    && (error.status === undefined || [502, 503, 504].includes(error.status));
}

async function withFallback<T>(
  loader: () => Promise<T>,
  fallbackData: T,
): Promise<QueryPayload<T>> {
  try {
    return { data: await loader(), fallback: false };
  } catch (error) {
    if (!isBackendUnavailable(error)) {
      throw error;
    }
    return {
      data: fallbackData,
      fallback: true,
      fallbackReason: 'Backend unavailable. Showing fallback demonstration data.',
    };
  }
}

export const sourceQueries = {
  list: () => withFallback(
    async () => (await request<PageResponse<Source>>('/api/v1/sources?size=100')).content,
    fallbackSources,
  ),
  health: () => withFallback(
    () => request<SourceHealth[]>('/api/v1/sources/health'),
    fallbackSourceHealth,
  ),
  validate: (sourceId: string) => request<SourceValidation>(
    `/api/v1/sources/${sourceId}/validate`,
    { method: 'POST' },
  ),
};

export const documentQueries = {
  list: () => withFallback(
    async () => (await request<PageResponse<DocumentRecord>>('/api/v1/documents?size=100')).content,
    fallbackDocuments,
  ),
  jobs: () => withFallback(
    async () => (await request<PageResponse<DownloadJob>>('/api/v1/documents/jobs?size=100')).content,
    fallbackDownloadJobs,
  ),
};

export const schedulerQueries = {
  jobs: () => withFallback(
    async () => (await request<PageResponse<SchedulerJob>>('/api/v1/scheduler/jobs?size=100')).content,
    fallbackSchedulerJobs,
  ),
};

export const portfolioQueries = {
  summary: () => request<PortfolioSummary>('/api/v1/portfolio/summary'),
  holdings: (page = 0, size = 100) =>
    request<PageResponse<PortfolioHolding>>(`/api/v1/portfolio/holdings?page=${page}&size=${size}`),
  sectorAllocation: () =>
    request<PortfolioAllocation[]>('/api/v1/portfolio/allocation/sector'),
  instrumentAllocation: () =>
    request<PortfolioAllocation[]>('/api/v1/portfolio/allocation/instrument'),
  importJobs: (page = 0, size = 100) =>
    request<PageResponse<PortfolioImportJob>>(`/api/v1/portfolio/import-jobs?page=${page}&size=${size}`),
  importXlsx: (file: File) => {
    const body = new FormData();
    body.append('file', file);
    return request<PortfolioImportResponse>('/api/v1/portfolio/import', {
      method: 'POST',
      body,
    });
  },
};

export const priceFeedQueries = {
  latest: () => request<PriceSnapshot[]>('/api/v1/market/prices/latest'),
  updateManual: (payload: ManualPriceRequest) =>
    request<PriceSnapshot>('/api/v1/market/prices/manual', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    }),
  refreshMock: () => request<PriceFeedJob>('/api/v1/market/prices/mock-refresh', {
    method: 'POST',
  }),
  refreshReal: () => request<PriceFeedJob>('/api/v1/market/prices/refresh-real', {
    method: 'POST',
  }),
  providerStatus: () =>
    request<PriceProviderStatus>('/api/v1/market/prices/provider-status'),
};

export const aiQueries = {
  ask: (payload: AiAskRequest) => request<AiAnswer>('/api/v1/ai/ask', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  }),
  embedDocument: (documentId: string) =>
    request<EmbeddingJob>(`/api/v1/ai/documents/${documentId}/embed`, {
      method: 'POST',
    }),
  chunks: (documentId: string) =>
    request<DocumentChunk[]>(`/api/v1/ai/documents/${documentId}/chunks`),
  answers: () => request<AiAnswer[]>('/api/v1/ai/answers'),
};

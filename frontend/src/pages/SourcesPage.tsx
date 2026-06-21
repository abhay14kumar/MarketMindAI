import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  CloseRounded,
  LinkRounded,
  ManageSearchRounded,
  VerifiedRounded,
} from '@mui/icons-material';
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Divider,
  Drawer,
  IconButton,
  Snackbar,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Tooltip,
  Typography,
} from '@mui/material';
import type { ChipProps } from '@mui/material';
import { sourceQueries } from '../api/client';
import type { Source, SourceHealth, SourceValidation } from '../api/types';
import { PageHeader } from '../components/PageHeader';
import { QueryState } from '../components/QueryState';
import { SectionCard } from '../components/SectionCard';
import { StatusChip } from '../components/StatusChip';
import { formatDateTime, formatEnum } from '../utils/format';

type Notification = {
  severity: 'success' | 'warning' | 'error';
  message: string;
};

function semanticColor(value: string): ChipProps['color'] {
  switch (value.toUpperCase()) {
    case 'SUCCESS':
    case 'SUPPORTED':
    case 'AVAILABLE':
    case 'REACHABLE':
      return 'success';
    case 'WARNING':
      return 'warning';
    case 'FAILED':
    case 'UNSUPPORTED':
    case 'NOT AVAILABLE':
    case 'UNREACHABLE':
      return 'error';
    default:
      return 'default';
  }
}

function ValidationChip({ value, label }: { value: string; label?: string }) {
  const color = semanticColor(value);
  return (
    <Chip
      size="small"
      label={label ?? formatEnum(value)}
      color={color}
      variant={color === 'default' ? 'outlined' : 'filled'}
    />
  );
}

function validationFromHealth(
  source: Source,
  health?: SourceHealth,
): SourceValidation {
  return {
    sourceId: source.id,
    sourceName: source.name,
    reachable: health?.available ?? false,
    httpStatus: health?.lastHttpStatus ?? null,
    latencyMs: health?.lastLatencyMs ?? health?.latencyMs ?? 0,
    robotsTxtAvailable: health?.robotsTxtAvailable ?? false,
    robotsTxtStatus: health?.robotsTxtStatus ?? null,
    pdfCapabilityStatus: health?.pdfCapabilityStatus ?? 'UNKNOWN',
    validationStatus: health?.status ?? 'UNKNOWN',
    message: health?.message ?? 'This source has not been validated yet.',
    validatedAt: health?.lastValidatedAt ?? health?.checkedAt ?? '',
  };
}

export function SourcesPage() {
  const queryClient = useQueryClient();
  const [validatingSourceIds, setValidatingSourceIds] = useState<Set<string>>(
    () => new Set(),
  );
  const [localResults, setLocalResults] = useState<Map<string, SourceValidation>>(
    () => new Map(),
  );
  const [details, setDetails] = useState<SourceValidation | null>(null);
  const [notification, setNotification] = useState<Notification | null>(null);

  const sourcesQuery = useQuery({ queryKey: ['sources'], queryFn: sourceQueries.list });
  const healthQuery = useQuery({ queryKey: ['source-health'], queryFn: sourceQueries.health });
  const validationMutation = useMutation({
    mutationFn: sourceQueries.validate,
    onMutate: (sourceId) => {
      setValidatingSourceIds((current) => new Set(current).add(sourceId));
      setNotification(null);
    },
    onSuccess: (result: SourceValidation) => {
      // Keep the mutation response visible immediately while backend read models refresh.
      setLocalResults((current) => new Map(current).set(result.sourceId, result));
      setDetails((current) => current?.sourceId === result.sourceId ? result : current);
      setNotification({
        severity: result.validationStatus === 'FAILED'
          ? 'error'
          : result.validationStatus === 'WARNING'
            ? 'warning'
            : 'success',
        message: `${formatEnum(result.validationStatus)}: ${result.message}`,
      });
      void Promise.all([
        queryClient.invalidateQueries({ queryKey: ['sources'] }),
        queryClient.invalidateQueries({ queryKey: ['source-health'] }),
        queryClient.invalidateQueries({ queryKey: ['source-capabilities'] }),
      ]);
    },
    onError: (error) => {
      setNotification({
        severity: 'error',
        message: error instanceof Error ? error.message : 'Source validation failed.',
      });
    },
    onSettled: (_data, _error, sourceId) => {
      setValidatingSourceIds((current) => {
        const next = new Set(current);
        next.delete(sourceId);
        return next;
      });
    },
  });

  const sources = sourcesQuery.data?.data ?? [];
  const health = healthQuery.data?.data ?? [];
  const healthBySource = new Map(health.map((item) => [item.sourceId, item]));
  const error = sourcesQuery.error ?? healthQuery.error;

  return (
    <>
      <PageHeader
        eyebrow="Data governance"
        title="Source registry"
        description="Registered financial-data providers, validation health, capabilities, and operating priorities."
      />
      <SectionCard eyebrow="Live registry" title="Sources">
        <QueryState
          loading={sourcesQuery.isPending || healthQuery.isPending}
          error={error instanceof Error ? error : null}
          empty={sources.length === 0}
          fallbackMessage={sourcesQuery.data?.fallbackReason ?? healthQuery.data?.fallbackReason}
          onRetry={() => {
            void sourcesQuery.refetch();
            void healthQuery.refetch();
          }}
        >
          <TableContainer>
            <Table sx={{ minWidth: 1780 }}>
              <TableHead>
                <TableRow>
                  <TableCell>Source Name</TableCell>
                  <TableCell>Type</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>HTTP Status</TableCell>
                  <TableCell>Reachable</TableCell>
                  <TableCell>Latency</TableCell>
                  <TableCell>robots.txt</TableCell>
                  <TableCell>robots.txt Status</TableCell>
                  <TableCell>PDF Capability</TableCell>
                  <TableCell>Validation Status</TableCell>
                  <TableCell>Last Validated</TableCell>
                  <TableCell align="right">Reliability</TableCell>
                  <TableCell align="right">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {sources.map((source) => {
                  const sourceHealth = healthBySource.get(source.id);
                  const localResult = localResults.get(source.id);
                  const result = localResult ?? validationFromHealth(source, sourceHealth);
                  const isValidating = validatingSourceIds.has(source.id);
                  const hasValidation = Boolean(localResult || sourceHealth?.lastValidatedAt);

                  return (
                    <TableRow key={source.id} hover>
                      <TableCell>
                        <Stack>
                          <Typography fontWeight={650}>{source.name}</Typography>
                          <Typography
                            component="a"
                            href={source.baseUrl}
                            target="_blank"
                            rel="noreferrer"
                            variant="caption"
                            color="text.secondary"
                          >
                            <LinkRounded sx={{ fontSize: 12, verticalAlign: 'middle', mr: 0.5 }} />
                            {source.code}
                          </Typography>
                        </Stack>
                      </TableCell>
                      <TableCell>{formatEnum(source.sourceType)}</TableCell>
                      <TableCell><StatusChip label={source.status} /></TableCell>
                      <TableCell>{result.httpStatus ?? '—'}</TableCell>
                      <TableCell>
                        {hasValidation
                          ? <ValidationChip value={result.reachable ? 'REACHABLE' : 'UNREACHABLE'} />
                          : <ValidationChip value="UNKNOWN" />}
                      </TableCell>
                      <TableCell>{hasValidation ? `${result.latencyMs} ms` : '—'}</TableCell>
                      <TableCell>
                        {hasValidation
                          ? (
                            <ValidationChip
                              value={result.robotsTxtAvailable ? 'AVAILABLE' : 'NOT AVAILABLE'}
                              label={result.robotsTxtAvailable ? 'Available' : 'Not Available'}
                            />
                          )
                          : <ValidationChip value="UNKNOWN" />}
                      </TableCell>
                      <TableCell>{result.robotsTxtStatus ?? '—'}</TableCell>
                      <TableCell>
                        <ValidationChip value={result.pdfCapabilityStatus} />
                      </TableCell>
                      <TableCell>
                        <ValidationChip value={hasValidation ? result.validationStatus : 'UNKNOWN'} />
                      </TableCell>
                      <TableCell>
                        <Tooltip title={result.validatedAt ? formatDateTime(result.validatedAt) : ''}>
                          <Typography variant="body2">
                            {localResult ? 'Just now' : formatDateTime(result.validatedAt)}
                          </Typography>
                        </Tooltip>
                      </TableCell>
                      <TableCell align="right">{Math.round(source.reliabilityScore * 100)}%</TableCell>
                      <TableCell align="right">
                        <Stack direction="row" gap={1} justifyContent="flex-end">
                          <Button
                            size="small"
                            variant="text"
                            startIcon={<ManageSearchRounded />}
                            onClick={() => setDetails(result)}
                          >
                            View Details
                          </Button>
                          <Button
                            size="small"
                            variant="outlined"
                            startIcon={isValidating
                              ? <CircularProgress size={15} color="inherit" />
                              : <VerifiedRounded />}
                            disabled={isValidating}
                            onClick={() => validationMutation.mutate(source.id)}
                          >
                            {isValidating ? 'Validating…' : 'Validate'}
                          </Button>
                        </Stack>
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </TableContainer>
        </QueryState>
      </SectionCard>

      <Drawer
        anchor="right"
        open={details !== null}
        onClose={() => setDetails(null)}
        PaperProps={{
          sx: {
            width: { xs: '100%', sm: 480 },
            bgcolor: 'background.paper',
            p: 3,
          },
        }}
      >
        {details && (
          <Stack gap={2.5}>
            <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
              <Box>
                <Typography variant="overline" color="primary.main">Validation evidence</Typography>
                <Typography variant="h2" sx={{ mt: 0.5 }}>{details.sourceName}</Typography>
              </Box>
              <IconButton onClick={() => setDetails(null)} aria-label="Close details">
                <CloseRounded />
              </IconButton>
            </Stack>
            <Divider />
            <Stack gap={1.5}>
              <Detail label="Source ID" value={details.sourceId} />
              <Detail label="Source name" value={details.sourceName} />
              <Detail label="Reachable" value={details.reachable ? 'Yes' : 'No'} />
              <Detail label="HTTP status" value={details.httpStatus ?? '—'} />
              <Detail label="Latency" value={`${details.latencyMs} ms`} />
              <Detail label="robots.txt available" value={details.robotsTxtAvailable ? 'Yes' : 'No'} />
              <Detail label="robots.txt status" value={details.robotsTxtStatus ?? '—'} />
              <Detail label="PDF capability" value={formatEnum(details.pdfCapabilityStatus)} />
              <Detail label="Validation status" value={formatEnum(details.validationStatus)} />
              <Detail label="Validated at" value={formatDateTime(details.validatedAt)} />
            </Stack>
            <Divider />
            <Box>
              <Typography variant="overline" color="text.secondary">Message</Typography>
              <Typography sx={{ mt: 1, lineHeight: 1.7 }}>{details.message}</Typography>
            </Box>
          </Stack>
        )}
      </Drawer>

      <Snackbar
        open={notification !== null}
        autoHideDuration={7000}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        onClose={() => setNotification(null)}
      >
        <Alert
          severity={notification?.severity ?? 'success'}
          variant="filled"
          onClose={() => setNotification(null)}
          sx={{ maxWidth: 620 }}
        >
          {notification?.message}
        </Alert>
      </Snackbar>
    </>
  );
}

function Detail({ label, value }: { label: string; value: string | number }) {
  return (
    <Stack direction="row" justifyContent="space-between" gap={3}>
      <Typography variant="body2" color="text.secondary">{label}</Typography>
      <Typography
        variant="body2"
        textAlign="right"
        sx={{ fontFamily: '"IBM Plex Mono", monospace', wordBreak: 'break-word' }}
      >
        {value}
      </Typography>
    </Stack>
  );
}

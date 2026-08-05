import React, { useState } from 'react';
import { QueryClient, QueryClientProvider, useQueryClient } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { UseEffectComponent } from './components/UseEffectApproach';
import { TanStackQueryComponent } from './components/TanStackQueryApproach';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 2,
      refetchOnWindowFocus: true,
    },
  },
});

function MainApp() {
  const [showUseEffect, setShowUseEffect] = useState(true);
  const [showTanStack, setShowTanStack] = useState(true);
  const [multiComponents, setMultiComponents] = useState(false);

  const qc = useQueryClient();

  return (
    <div className="max-w-5xl mx-auto p-6">
      <header className="mb-8 border-b border-slate-800 pb-4">
        <h1 className="text-2xl font-bold text-sky-400">
          Ôn Tập Day 2: Quản Lý Server State (React Query vs useState/useEffect)
        </h1>
        <p className="text-slate-400 text-sm mt-1">
          Đề thi tự luận số 6 - TanStack Query, Caching, Request Deduplication & Server State.
        </p>
      </header>

      {/* Control Panel */}
      <div className="bg-slate-800/60 p-4 rounded-xl mb-6 flex flex-wrap gap-4 items-center justify-between">
        <div className="flex gap-3">
          <button
            onClick={() => setShowUseEffect(!showUseEffect)}
            className="px-3 py-1.5 bg-rose-600 hover:bg-rose-500 rounded text-sm font-medium transition"
          >
            {showUseEffect ? 'Ẩn' : 'Bật'} useState Demo
          </button>
          <button
            onClick={() => setShowTanStack(!showTanStack)}
            className="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-500 rounded text-sm font-medium transition"
          >
            {showTanStack ? 'Ẩn' : 'Bật'} TanStack Query Demo
          </button>
          <button
            onClick={() => setMultiComponents(!multiComponents)}
            className="px-3 py-1.5 bg-indigo-600 hover:bg-indigo-500 rounded text-sm font-medium transition"
          >
            {multiComponents ? 'Tắt' : 'Bật'} Test Deduplication (3 Components)
          </button>
        </div>

        <button
          onClick={() => qc.invalidateQueries({ queryKey: ['users'] })}
          className="px-3 py-1.5 bg-amber-600 hover:bg-amber-500 text-sm font-medium rounded transition"
        >
          ⚡ Invalidate Cache (Buộc TanStack Refetch)
        </button>
      </div>

      {/* Grid Comparison */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Section 1: Traditional useEffect */}
        <div className="space-y-4">
          <h2 className="text-lg font-semibold text-rose-400 border-b border-rose-900/50 pb-2">
            1. Cách truyền thống (useState + useEffect)
          </h2>
          {showUseEffect ? (
            <div className="space-y-3">
              <UseEffectComponent name="Component A" />
              {multiComponents && (
                <>
                  <UseEffectComponent name="Component B" />
                  <UseEffectComponent name="Component C" />
                </>
              )}
            </div>
          ) : (
            <div className="p-8 text-center border border-dashed border-slate-700 rounded-xl text-slate-500">
              Component bị Unmounted. Bấm lại nút trên để Mount lại (Sẽ phải fetch API từ đầu!).
            </div>
          )}
        </div>

        {/* Section 2: TanStack Query */}
        <div className="space-y-4">
          <h2 className="text-lg font-semibold text-emerald-400 border-b border-emerald-900/50 pb-2">
            2. Cách hiện đại (TanStack Query)
          </h2>
          {showTanStack ? (
            <div className="space-y-3">
              <TanStackQueryComponent name="Component X" />
              {multiComponents && (
                <>
                  <TanStackQueryComponent name="Component Y" />
                  <TanStackQueryComponent name="Component Z" />
                </>
              )}
            </div>
          ) : (
            <div className="p-8 text-center border border-dashed border-slate-700 rounded-xl text-slate-500">
              Component bị Unmounted. Bấm lại nút trên để Mount lại (Hiển thị ngay từ Cache!).
            </div>
          )}
        </div>
      </div>

      {/* Summary Note */}
      <div className="mt-8 p-4 bg-slate-800/40 border border-slate-700 rounded-xl text-sm space-y-2">
        <h4 className="font-bold text-sky-300">💡 Ghi chú kiểm thử (xem F12 / Console tab):</h4>
        <ul className="list-disc list-inside space-y-1 text-slate-300">
          <li>
            <strong>Deduplication:</strong> Bật nút <em>"Test Deduplication"</em>. Các Component X, Y, Z bên phía TanStack Query chỉ gửi <strong>đúng 1 HTTP Request</strong> trong Console, trong khi Component A, B, C gửi 3 requests.
          </li>
          <li>
            <strong>Caching & Revalidation:</strong> Tắt rồi Bật lại nút TanStack Query. Dữ liệu hiện ra lập tức không có delay.
          </li>
        </ul>
      </div>
    </div>
  );
}

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <MainApp />
      <ReactQueryDevtools initialIsOpen={false} />
    </QueryClientProvider>
  );
}

import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { fetchUsers } from '../api/usersApi';

/**
 * Minh họa TanStack Query:
 * - Caching: Khi chuyển trang rồi quay lại, dữ liệu lập tức hiển thị từ cache.
 * - Deduplication: Nhiều component gọi chung 1 key -> Chỉ bắn 1 HTTP Request.
 * - Revalidation: Tự động cập nhật ngầm.
 */
export const TanStackQueryComponent = ({ name }) => {
  const { data: users, isLoading, isFetching, isError, error } = useQuery({
    queryKey: ['users'],
    queryFn: fetchUsers,
    staleTime: 5000, // Dữ liệu coi là tươi trong 5s
  });

  return (
    <div className="p-4 border border-emerald-500/30 bg-emerald-950/20 rounded-xl">
      <div className="flex items-center justify-between mb-2">
        <h3 className="font-semibold text-emerald-400">
          🟢 {name} (dùng TanStack Query)
        </h3>
        {isFetching && !isLoading && (
          <span className="text-xs text-amber-400 bg-amber-950/50 px-2 py-0.5 rounded">
            🔄 Refetching ngầm...
          </span>
        )}
      </div>

      {isLoading ? (
        <div className="text-slate-400 text-sm animate-pulse">
          ⏳ Đang tải dữ liệu ban đầu...
        </div>
      ) : isError ? (
        <div className="text-red-400 text-sm">❌ Lỗi: {error?.message}</div>
      ) : (
        <ul className="text-sm space-y-1">
          {users?.map((u) => (
            <li key={u.id} className="text-slate-300">
              • {u.name} ({u.role})
            </li>
          ))}
        </ul>
      )}
    </div>
  );
};

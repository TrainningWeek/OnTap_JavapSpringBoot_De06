import React, { useState, useEffect } from 'react';
import { fetchUsers } from '../api/usersApi';

/**
 * Minh họa cách làm cũ: dùng useEffect + useState
 * - Mỗi khi component mount -> Fetch lại API từ đầu (không có cache).
 * - Nếu 2 component gọi -> Bắn 2 HTTP requests riêng biệt (không deduplication).
 * - Phải tự viết tay isLoading, error state thủ công.
 */
export const UseEffectComponent = ({ name }) => {
  const [users, setUsers] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let isMounted = true;
    setIsLoading(true);

    fetchUsers()
      .then((data) => {
        if (isMounted) {
          setUsers(data);
          setIsLoading(false);
        }
      })
      .catch((err) => {
        if (isMounted) {
          setError(err.message);
          setIsLoading(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, []);

  return (
    <div className="p-4 border border-rose-500/30 bg-rose-950/20 rounded-xl">
      <h3 className="font-semibold text-rose-400 mb-2">
        🔴 {name} (dùng useState + useEffect)
      </h3>
      {isLoading ? (
        <div className="text-slate-400 text-sm animate-pulse">
          ⏳ Đang tải dữ liệu từ Server...
        </div>
      ) : error ? (
        <div className="text-red-400 text-sm">❌ Lỗi: {error}</div>
      ) : (
        <ul className="text-sm space-y-1">
          {users.map((u) => (
            <li key={u.id} className="text-slate-300">
              • {u.name} ({u.role})
            </li>
          ))}
        </ul>
      )}
    </div>
  );
};

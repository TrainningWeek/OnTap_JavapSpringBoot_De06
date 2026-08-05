// Mock API để giả lập gọi dữ liệu từ Server có độ trễ (delay)
let initialUsers = [
  { id: 1, name: 'Nguyễn Văn A', email: 'a.nguyen@example.com', role: 'Admin' },
  { id: 2, name: 'Trần Thị B', email: 'b.tran@example.com', role: 'User' },
  { id: 3, name: 'Lê Văn C', email: 'c.le@example.com', role: 'User' },
  { id: 4, name: 'Phạm Hoàng D', email: 'd.pham@example.com', role: 'Manager' },
];

let requestCounter = 0;

export const fetchUsers = async () => {
  requestCounter++;
  const currentReq = requestCounter;
  console.log(`[API Server] HTTP GET /api/users - Request #${currentReq}`);

  // Giả lập trễ 1 giây
  await new Promise((resolve) => setTimeout(resolve, 1000));

  return [...initialUsers];
};

export const addUser = async (newUser) => {
  await new Promise((resolve) => setTimeout(resolve, 800));
  const user = { id: Date.now(), ...newUser };
  initialUsers.push(user);
  return user;
};

export const getRequestCount = () => requestCounter;

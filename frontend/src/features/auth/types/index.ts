export interface OrganizationInfo { institutionId:number;institutionName:string;institutionType:string;facultyId:number|null;facultyName:string|null }
export interface Account { id: number; username: string; email: string; status: string; emailVerified: boolean; roles: string[]; createdAt: string; lastLoginAt: string | null; organization: OrganizationInfo | null; displayName:string|null; bio:string|null; avatarUrl:string|null }
export interface AccountPage { content: Account[]; totalElements: number; totalPages: number; page: number; size: number }
export const roleNames: Record<string, string> = { ADMIN: 'Quản trị viên', MODERATOR: 'Kiểm duyệt viên', USER: 'Thành viên' };
export const statusNames: Record<string, string> = { ACTIVE: 'Hoạt động', INACTIVE: 'Tạm ngưng', LOCKED: 'Đã khóa', DELETED: 'Đã xóa mềm' };
export function dateLabel(value: string | null) { return value ? new Date(value + (/[Z+]/.test(value) ? '' : 'Z')).toLocaleString('vi-VN') : 'Chưa có'; }

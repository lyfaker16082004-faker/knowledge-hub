export interface ApiResponse<T> { success: boolean; message: string; data: T }
export interface PageResponse<T> { content: T[]; totalElements: number; totalPages: number; page: number; size: number }
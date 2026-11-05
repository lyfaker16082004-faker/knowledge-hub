export interface DocumentItem { id: number; slug: string; title: string; summary: string; categoryId: number; category: string; accessType: 'FREE' | 'PAID'; price: number; currency: string; level: string; language: string; coverStyle: string; featured: boolean; publishedAt: string; downloads: number;institutionId:number|null;institutionName:string|null;facultyId:number|null;facultyName:string|null;fileFormat:string;subjectId:number|null;subjectName:string|null;tags:string[] }
export interface PageData<T> { content: T[]; totalElements: number; totalPages: number; page: number; size: number }
export interface CategoryInfo { id: number; name: string; slug: string; count: number }
export interface CatalogueStats { documents: number; freeDocuments: number; categories: number; downloads: number;institutionId:number|null;institutionName:string|null;facultyId:number|null;facultyName:string|null;fileFormat:string }
export interface DocumentDetail { document: DocumentItem; description: string; author: string; saved: boolean; canDownload: boolean; status:string;reviewNote:string|null;version:number|null;canManage:boolean;canPreview:boolean;files: { id: number; name: string; contentType: string; sizeBytes: number }[] }
export interface DownloadInfo { id: number; documentId: number; title: string; category: string; fileName: string; downloadedAt: string; available: boolean }
export interface PersonalStats { downloadedDocuments: number; downloads: number; savedDocuments: number }
export const levelNames: Record<string,string> = { BEGINNER: 'Cơ bản', INTERMEDIATE: 'Trung cấp', ADVANCED: 'Nâng cao' };
export const priceLabel = (doc: DocumentItem) => doc.accessType === 'FREE' ? 'Miễn phí' : new Intl.NumberFormat('vi-VN', { style: 'currency', currency: doc.currency }).format(doc.price);
export const fileSize = (value: number) => value < 1024 ? `${value} B` : value < 1048576 ? `${(value / 1024).toFixed(1)} KB` : `${(value / 1048576).toFixed(1)} MB`;

export const formatName=(format:string)=>({MD:'Markdown',TXT:'Văn bản',PDF:'PDF',DOCX:'Word'}[format]||format);

export interface ManagedDocument{document:DocumentItem;description:string;authorId:number|null;author:string;status:string;reviewNote:string|null;createdAt:string;updatedAt:string;version:number}
export const documentStatuses:Record<string,string>={PENDING:'Chờ duyệt',PUBLISHED:'Đã công bố',REJECTED:'Bị từ chối',ARCHIVED:'Đã ẩn',DRAFT:'Bản nháp',DELETED:'Đã xóa'};

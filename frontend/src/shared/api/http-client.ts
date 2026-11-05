import type { ApiResponse } from './types';
const baseUrl=import.meta.env.VITE_API_BASE_URL||'/api';
const tokenKey='knowledge-hub.access-token';
export class ApiError extends Error{constructor(public status:number,message:string){super(message);}}
export function setAccessToken(token:string|null){try{if(token)sessionStorage.setItem(tokenKey,token);else sessionStorage.removeItem(tokenKey);}catch{/* A restricted browser can require signing in again. */}}
export function getAccessToken(){try{const token=sessionStorage.getItem(tokenKey);if(!token)return null;const raw=token.split('.')[1];if(!raw)throw new Error('Invalid JWT');let encoded=raw.replace(/-/g,'+').replace(/_/g,'/');encoded=encoded.padEnd(Math.ceil(encoded.length/4)*4,'=');const claims=JSON.parse(atob(encoded));if(!claims.exp||claims.exp*1000<=Date.now()){setAccessToken(null);window.dispatchEvent(new Event('auth-expired'));return null;}return token;}catch{setAccessToken(null);return null;}}
function authorization():Record<string,string>{const token=getAccessToken();return token?{Authorization:`Bearer ${token}`}:{ };}
function expired(){setAccessToken(null);window.dispatchEvent(new Event('auth-expired'));}
async function read<T>(path:string,init:RequestInit={}):Promise<T>{
 let response:Response;try{const headers=new Headers(init.headers);if(!['/auth/login','/auth/register'].includes(path))for(const [name,value] of Object.entries(authorization()))headers.set(name,value);response=await fetch(`${baseUrl}${path}`,{...init,credentials:'omit',headers});}catch(error){if(error instanceof DOMException&&error.name==='AbortError')throw error;throw new ApiError(0,'Không thể kết nối máy chủ. Hãy kiểm tra backend và thử lại.');}
 const result=await response.json().catch(()=>null) as ApiResponse<T>|null;
 if(!response.ok){if(response.status===401&&!path.startsWith('/auth/login'))expired();throw new ApiError(response.status,result?.message||`Yêu cầu thất bại (${response.status}). Vui lòng thử lại.`);}return result?.data as T;
}
export function get<T>(path:string,signal?:AbortSignal){return read<T>(path,{signal});}
export function mutate<T>(path:string,method:'POST'|'PUT'|'DELETE',data?:unknown,form=false){return read<T>(path,{method,headers:{'Content-Type':form?'application/x-www-form-urlencoded':'application/json'},body:form?new URLSearchParams(data as Record<string,string>).toString():data===undefined?undefined:JSON.stringify(data)});}
export function errorMessage(error:unknown){return error instanceof Error?error.message:'Có lỗi xảy ra. Vui lòng thử lại.';}
export async function downloadFile(documentId:number,fileId:number,fileName:string){
 const response=await fetch(`${baseUrl}/documents/${documentId}/files/${fileId}/download`,{method:'POST',credentials:'omit',headers:authorization()});
 if(!response.ok){const result=await response.json().catch(()=>null);if(response.status===401)expired();throw new ApiError(response.status,result?.message||'Không thể tải tài liệu. Vui lòng thử lại.');}
 const blob=await response.blob();const url=URL.createObjectURL(blob);const link=document.createElement('a');link.href=url;link.download=fileName;document.body.appendChild(link);link.click();link.remove();setTimeout(()=>URL.revokeObjectURL(url),1000);
}
export async function uploadMultipart<T>(path:string,form:FormData):Promise<T>{
 let response:Response;try{response=await fetch(`${baseUrl}${path}`,{method:'POST',credentials:'omit',headers:authorization(),body:form});}catch{throw new ApiError(0,'Không thể kết nối máy chủ để tải tệp.');}
 const result=await response.json().catch(()=>null);if(!response.ok){if(response.status===401)expired();throw new ApiError(response.status,result?.message||'Không thể tải tệp. Vui lòng thử lại.');}return result.data as T;
}
export function uploadDocument<T>(metadata:unknown,file:File){const form=new FormData();form.append('metadata',new Blob([JSON.stringify(metadata)],{type:'application/json'}));form.append('file',file);return uploadMultipart<T>('/documents',form);}
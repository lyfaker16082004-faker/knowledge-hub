import {get,mutate,setAccessToken} from '../../../shared/api/http-client';
import type {Account} from '../types';
export interface LoginResult{accessToken:string;tokenType:string;expiresIn:number;user:Account}
export const currentAccount=()=>get<Account>('/users/me');
export async function login(username:string,password:string){const result=await mutate<LoginResult>('/auth/login','POST',{username,password});setAccessToken(result.accessToken);return result.user;}
export async function register(username:string,email:string,password:string,institutionId:number|null=null,facultyId:number|null=null){return mutate<Account>('/auth/register','POST',{username:username.trim()||undefined,email,password,institutionId,facultyId});}
export async function logout(){await mutate('/auth/logout','POST');setAccessToken(null);}
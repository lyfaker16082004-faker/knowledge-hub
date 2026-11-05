import { useCallback, useEffect, useState } from 'react';
import { get, errorMessage } from '../api/http-client';
export function useRemote<T>(path: string|null, dependency = '') {
 const [data,setData]=useState<T|null>(null);const [error,setError]=useState('');const [loading,setLoading]=useState(true);const [version,setVersion]=useState(0);
 const refresh=useCallback(()=>setVersion(n=>n+1),[]);
 useEffect(()=>{if(path===null){setData(null);setError('');setLoading(false);return;}const controller=new AbortController();setLoading(true);setError('');setData(null);get<T>(path,controller.signal).then(setData).catch(e=>{if(!controller.signal.aborted)setError(errorMessage(e));}).finally(()=>{if(!controller.signal.aborted)setLoading(false);});return()=>controller.abort();},[path,version,dependency]);
 return {data,error,loading,refresh};
}

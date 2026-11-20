const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
let accessToken = null, csrfToken = null
export const setAccessToken = token => { accessToken = token }
async function request(path, options={}) {
 const headers={Accept:'application/json',...(options.body?{'Content-Type':'application/json'}:{}),...(accessToken?{Authorization:`Bearer ${accessToken}`}:{})}
 if(csrfToken && !['GET','HEAD','OPTIONS'].includes((options.method||'GET').toUpperCase())) headers['X-XSRF-TOKEN']=csrfToken
 const res=await fetch(`${API_BASE}${path}`,{...options,headers:{...headers,...options.headers},credentials:'include'})
 const data=res.status===204?null:await res.json().catch(()=>null)
 if(!res.ok) throw new Error(data?.detail||data?.message||data?.error||`Request failed (${res.status})`)
 return data
}
export const api={
 csrf:async()=>{const d=await request('/api/auth/csrf');csrfToken=d.token;return d},
 register:async(name,email,password)=>{await api.csrf();return request('/api/auth/register',{method:'POST',body:JSON.stringify({name,email,password})})},
 login:async(email,password)=>{await api.csrf();return request('/api/auth/login',{method:'POST',body:JSON.stringify({email,password})})},
 refresh:async()=>{await api.csrf();return request('/api/auth/refresh',{method:'POST'})},
 logout:()=>request('/api/auth/logout',{method:'POST'}),
 me:()=>request('/api/auth/me'),
 updateProfile:name=>request('/api/users/me',{method:'PUT',body:JSON.stringify({name})}),
 security:()=>request('/api/users/me/security'),
 walletNonce:async(address,chainId,purpose='LOGIN')=>{await api.csrf();return request('/api/auth/wallet/nonce',{method:'POST',body:JSON.stringify({address,chainId,purpose})})},
 walletVerify:async(address,message,signature)=>{await api.csrf();return request('/api/auth/wallet/verify',{method:'POST',body:JSON.stringify({address,message,signature})})},
 walletLinkVerify:async(address,message,signature)=>{await api.csrf();return request('/api/auth/wallet/link/verify',{method:'POST',body:JSON.stringify({address,message,signature})})},
 wallets:()=>request('/api/users/me/wallets'),
 unlinkWallet:id=>request(`/api/users/me/wallets/${id}`,{method:'DELETE'}),
 changePassword:(currentPassword,newPassword)=>request('/api/users/me/change-password',{method:'POST',body:JSON.stringify({currentPassword,newPassword})}),
 adminUsers:()=>request('/api/admin/users'),adminUpdateRoles:(id,roles)=>request(`/api/admin/users/${id}/roles`,{method:'PUT',body:JSON.stringify({roles})}),adminSuspend:id=>request(`/api/admin/users/${id}/suspend`,{method:'POST'}),adminReactivate:id=>request(`/api/admin/users/${id}/reactivate`,{method:'POST'}),adminAudit:()=>request('/api/admin/audit?limit=100'),adminAuditVerify:()=>request('/api/admin/audit/verify'),
 health:()=>request('/api/health'),databaseHealth:()=>request('/api/health/database'),blockchainHealth:()=>request('/api/health/blockchain')
}

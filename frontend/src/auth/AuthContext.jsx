import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import { api, setAccessToken } from '../api/client'
const AuthContext = createContext(null)
export function AuthProvider({ children }) {
 const [user,setUser]=useState(null),[ready,setReady]=useState(false)
 useEffect(()=>{(async()=>{try{await api.csrf();const session=await api.refresh();setAccessToken(session.accessToken);setUser(session.user)}catch{setAccessToken(null)}finally{setReady(true)}})()},[])
 const value=useMemo(()=>({user,ready,async login(email,password){const r=await api.login(email,password);setAccessToken(r.accessToken);setUser(r.user);return r},acceptSession(r){setAccessToken(r.accessToken);setUser(r.user)},async register(name,email,password){return api.register(name,email,password)},async logout(){try{await api.logout()}finally{setAccessToken(null);setUser(null)}},async updateProfile(name){const u=await api.updateProfile(name);setUser(u);return u}}),[user,ready])
 return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
export const useAuth=()=>useContext(AuthContext)

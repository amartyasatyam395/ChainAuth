import { useEffect, useState } from 'react'
import { BrowserProvider } from 'ethers'
import { api } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { useNavigate } from 'react-router-dom'
export default function WalletSignIn(){
 const {acceptSession}=useAuth(),navigate=useNavigate()
 const [busy,setBusy]=useState(false),[error,setError]=useState(''),[walletNotice,setWalletNotice]=useState('')
 useEffect(()=>{
  const eth=window.ethereum;if(!eth?.on)return
  const accounts=()=>setWalletNotice('Wallet account changed. Restart sign-in to use the selected account.')
  const chain=()=>setWalletNotice('Wallet network changed. Restart sign-in on the configured network.')
  const disconnect=()=>setWalletNotice('Wallet provider disconnected.')
  eth.on('accountsChanged',accounts);eth.on('chainChanged',chain);eth.on('disconnect',disconnect)
  return ()=>{eth.removeListener?.('accountsChanged',accounts);eth.removeListener?.('chainChanged',chain);eth.removeListener?.('disconnect',disconnect)}
 },[])
 async function signIn(){
  setError('')
  if(!window.ethereum){setError('MetaMask was not detected. Install the MetaMask browser extension.');return}
  setBusy(true)
  try{
   const provider=new BrowserProvider(window.ethereum),accounts=await provider.send('eth_requestAccounts',[])
   if(!accounts.length)throw new Error('No wallet account was selected.')
   const signer=await provider.getSigner(),address=await signer.getAddress(),network=await provider.getNetwork()
   const challenge=await api.walletNonce(address,Number(network.chainId),'LOGIN')
   const signature=await signer.signMessage(challenge.message)
   if((await signer.getAddress()).toLowerCase()!==address.toLowerCase()||Number((await provider.getNetwork()).chainId)!==Number(network.chainId))throw new Error('Wallet account or network changed during signing. Please retry.')
   const session=await api.walletVerify(address,challenge.message,signature)
   acceptSession(session);navigate('/dashboard')
  }catch(e){setError(e.code===4001?'Wallet signature request was rejected.':e.message||'Wallet authentication failed.')}
  finally{setBusy(false)}
 }
 return <div className="wallet-auth"><div className="auth-divider"><span>OR</span></div>
 <button type="button" className="wallet-button" onClick={signIn} disabled={busy}>{busy?'Waiting for wallet…':'◇  Sign in with MetaMask'}</button>
 {walletNotice&&<p className="muted" role="status">{walletNotice}</p>}{error&&<p className="form-error" role="alert">{error}</p>}
 <p className="muted wallet-note">A gas-free SIWE signature proves wallet ownership. ChainAuth never asks for your seed phrase or private key.</p></div>
}

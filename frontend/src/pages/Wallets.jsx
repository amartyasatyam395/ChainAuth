import { useEffect,useState } from 'react'
import { BrowserProvider } from 'ethers'
import { Link } from 'react-router-dom'
import { api } from '../api/client'
export default function Wallets(){
 const [wallets,setWallets]=useState([]),[busy,setBusy]=useState(false),[message,setMessage]=useState(''),[error,setError]=useState(''),[walletNotice,setWalletNotice]=useState('')
 async function load(){try{setWallets(await api.wallets())}catch(e){setError(e.message)}}
 useEffect(()=>{load()},[])
 useEffect(()=>{
  const eth=window.ethereum;if(!eth?.on)return
  const accounts=()=>setWalletNotice('MetaMask account selection changed.')
  const chain=()=>setWalletNotice('MetaMask network changed. Check the configured network.')
  const disconnect=()=>setWalletNotice('MetaMask disconnected.')
  eth.on('accountsChanged',accounts);eth.on('chainChanged',chain);eth.on('disconnect',disconnect)
  return ()=>{eth.removeListener?.('accountsChanged',accounts);eth.removeListener?.('chainChanged',chain);eth.removeListener?.('disconnect',disconnect)}
 },[])
 async function linkWallet(){
  setBusy(true);setError('');setMessage('')
  try{
   if(!window.ethereum)throw new Error('MetaMask was not detected.')
   const provider=new BrowserProvider(window.ethereum);await provider.send('eth_requestAccounts',[])
   const signer=await provider.getSigner(),address=await signer.getAddress(),network=await provider.getNetwork()
   const challenge=await api.walletNonce(address,Number(network.chainId),'LINK')
   const signature=await signer.signMessage(challenge.message)
   if((await signer.getAddress()).toLowerCase()!==address.toLowerCase()||Number((await provider.getNetwork()).chainId)!==Number(network.chainId))throw new Error('Wallet account or network changed during signing. Please retry.')
   const result=await api.walletLinkVerify(address,challenge.message,signature)
   setMessage(result.message);await load()
  }catch(e){setError(e.code===4001?'Wallet signature request was rejected.':e.message)}
  finally{setBusy(false)}
 }
 async function unlink(id){setError('');setMessage('');try{const r=await api.unlinkWallet(id);setMessage(r.message);await load()}catch(e){setError(e.message)}}
 return <main className="dashboard"><Link to="/dashboard">← Dashboard</Link><p className="kicker">WALLET AUTHENTICATION</p><h1>Linked wallets</h1>
 <p className="muted">Wallets linked to this ChainAuth account. Linking requires a fresh signature.</p>
 <button onClick={linkWallet} disabled={busy}>{busy?'Waiting for MetaMask…':'＋ Link a wallet'}</button>
 {walletNotice&&<p className="muted">{walletNotice}</p>}{message&&<p className="success-message">{message}</p>}{error&&<p className="form-error">{error}</p>}
 <div className="wallet-list">{wallets.map(w=><section className="status-card" key={w.id}><p className="eyebrow">ETHEREUM · CHAIN {w.chainId}</p><h3 className="wallet-address">{w.address}</h3><p className="muted">Linked {new Date(w.linkedAt).toLocaleString()}</p><button className="secondary" onClick={()=>unlink(w.id)}>Unlink wallet</button></section>)}
 {wallets.length===0&&<section className="status-card"><p className="muted">No wallets linked yet.</p></section>}</div></main>
}

import { ConfigProvider } from 'antd'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './App'
import './index.css'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ConfigProvider
      theme={{
        token: {
          colorPrimary: '#2f6fed',
          colorInfo: '#1a3358',
          borderRadius: 8,
          fontFamily: "'Manrope', system-ui, sans-serif",
        },
      }}
    >
      <App />
    </ConfigProvider>
  </StrictMode>,
)

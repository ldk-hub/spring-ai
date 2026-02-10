import { useState, useRef, useEffect } from 'react'

function App() {
  const [messages, setMessages] = useState([
    { text: '안녕하세요! 무엇을 도와드릴까요?', isUser: false }
  ])
  const [input, setInput] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [isSidebarOpen, setIsSidebarOpen] = useState(false) // Mobile sidebar state
  const messagesEndRef = useRef(null)

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }

  useEffect(() => {
    scrollToBottom()
  }, [messages])

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!input.trim() || isLoading) return

    const userMessage = input.trim()
    setInput('')
    setMessages(prev => [...prev, { text: userMessage, isUser: true }])
    setIsLoading(true)

    try {
      const response = await fetch('http://localhost:8080/api/chat', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ message: userMessage }),
      })

      if (!response.ok) {
        throw new Error('Network response was not ok')
      }

      const data = await response.json()
      setMessages(prev => [...prev, { text: data.response, isUser: false }])
    } catch (error) {
      console.error('Error:', error)
      setMessages(prev => [...prev, { text: '죄송합니다. 오류가 발생했습니다.', isUser: false }])
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="app-container">
      {/* Sidebar */}
      <aside className={`sidebar ${isSidebarOpen ? 'open' : ''}`}>
        <div className="sidebar-header">
          <span>Gemini Pro</span>
          <button className="menu-btn" onClick={() => setIsSidebarOpen(false)}>✕</button>
        </div>
        <button className="new-chat-btn">
          <span>+</span> 새로운 채팅
        </button>
        {/* Chat History could go here */}
      </aside>

      {/* Main Content */}
      <main className="main-content">
        {/* Header */}
        <header className="chat-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
            <button className="menu-btn" onClick={() => setIsSidebarOpen(true)}>
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M3 12H21" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                <path d="M3 6H21" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                <path d="M3 18H21" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
              </svg>
            </button>
            <div className="model-selector">
              Spring AI RAG <span style={{ fontSize: '0.8em', opacity: 0.6 }}>▼</span>
            </div>
          </div>
          {/* User Profile / Settings could go here */}
        </header>

        {/* Chat Area */}
        <div className="messages-area">
          {messages.map((msg, index) => (
            <div key={index} className="message-wrapper">
              {/* Icons */}
              <div className={`message-icon ${msg.isUser ? 'user-icon' : 'bot-icon'}`}>
                {msg.isUser ? 'U' : 'AI'}
              </div>

              <div className="message-content">
                <div className="message-sender">{msg.isUser ? 'User' : 'Assistant'}</div>
                {msg.text}
              </div>
            </div>
          ))}

          {isLoading && (
            <div className="message-wrapper">
              <div className="message-icon bot-icon">AI</div>
              <div className="message-content">
                <div className="message-sender">Assistant</div>
                <div className="loading-dots">
                  <div className="dot"></div>
                  <div className="dot"></div>
                  <div className="dot"></div>
                </div>
              </div>
            </div>
          )}
          <div ref={messagesEndRef} />
        </div>

        {/* Footer / Input Area */}
        <div className="input-area-wrapper">
          <form className="input-container" onSubmit={handleSubmit}>
            <input
              type="text"
              className="chat-input"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder="무엇을 도와드릴까요?"
              disabled={isLoading}
            />
            <button type="submit" className="send-btn" disabled={isLoading || !input.trim()}>
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M22 2L11 13" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                <path d="M22 2L15 22L11 13L2 9L22 2Z" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
            </button>
          </form>
        </div>
      </main>

      {/* Overlay for mobile when sidebar is open */}
      {isSidebarOpen && (
        <div
          style={{
            position: 'absolute', top: 0, left: 0, right: 0, bottom: 0,
            backgroundColor: 'rgba(0,0,0,0.5)', zIndex: 15
          }}
          onClick={() => setIsSidebarOpen(false)}
        />
      )}
    </div>
  )
}

export default App

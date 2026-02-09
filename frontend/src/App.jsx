import { useState, useRef, useEffect } from 'react'

function App() {
  const [messages, setMessages] = useState([
    { text: '안녕하세요! 무엇을 도와드릴까요?', isUser: false }
  ])
  const [input, setInput] = useState('')
  const [isLoading, setIsLoading] = useState(false)
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
    <div className="chat-container">
      <header className="chat-header">
        Spring AI RAG Chat
      </header>
      
      <div className="messages-area">
        {messages.map((msg, index) => (
          <div 
            key={index} 
            className={`message ${msg.isUser ? 'user' : 'bot'}`}
          >
            {msg.text}
          </div>
        ))}
        {isLoading && (
          <div className="message bot">
            <div className="loading-dots">
              <div className="dot"></div>
              <div className="dot"></div>
              <div className="dot"></div>
            </div>
          </div>
        )}
        <div ref={messagesEndRef} />
      </div>

      <form className="input-area" onSubmit={handleSubmit}>
        <input
          type="text"
          className="chat-input"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="메시지를 입력하세요..."
          disabled={isLoading}
        />
        <button type="submit" className="send-btn" disabled={isLoading || !input.trim()}>
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M22 2L11 13" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
            <path d="M22 2L15 22L11 13L2 9L22 2Z" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
          </svg>
        </button>
      </form>
    </div>
  )
}

export default App

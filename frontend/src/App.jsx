import { useState, useRef, useEffect } from 'react'

function App() {
  const [messages, setMessages] = useState([]) // Start empty for Welcome Screen
  const [input, setInput] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [isSidebarOpen, setIsSidebarOpen] = useState(false)
  const messagesEndRef = useRef(null)

  // Chat ID state for conversation history
  const [chatId, setChatId] = useState(() => {
    return localStorage.getItem('chatId') || crypto.randomUUID()
  })

  const [apiHealth, setApiHealth] = useState(null)

  useEffect(() => {
    // API 상태 확인
    fetch('/api/chat/health')
      .then(res => res.json())
      .then(data => setApiHealth(data.status === 'UP'))
      .catch(() => setApiHealth(false))
  }, [])

  useEffect(() => {
    localStorage.setItem('chatId', chatId)

    // 대화 이력 불러오기
    let ignore = false;

    const fetchHistory = async () => {
      try {
        const response = await fetch(`/api/chat/history?chatId=${chatId}`)
        if (response.ok) {
          const data = await response.json()
          if (!ignore) {
            if (data && data.length > 0) {
              const formattedMessages = data.map(msg => ({
                text: msg.content,
                isUser: msg.type === 'user' || msg.type === 'USER'
              }))
              setMessages(formattedMessages)
            } else {
              setMessages([])
            }
          }
        }
      } catch (error) {
        if (!ignore) console.error('이력 불러오기 실패:', error)
      }
    }

    if (chatId) {
      fetchHistory()
    }

    return () => {
      ignore = true;
    }
  }, [chatId])

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }

  useEffect(() => {
    scrollToBottom()
  }, [messages])

  // 메시지 전송 및 API 호출을 처리하는 핵심 함수
  const sendMessage = async (messageText) => {
    // 빈 메시지나 로딩 중일 경우 실행하지 않음
    if (!messageText.trim() || isLoading) return

    const userMessage = messageText.trim()
    setInput('')
    // 사용자 메시지를 화면에 즉시 표시
    setMessages(prev => [...prev, { text: userMessage, isUser: true }])
    setIsLoading(true)

    try {
      // API 호출: 상대 경로를 사용하여 배포 환경에서도 동작하도록 함
      const response = await fetch('/api/chat', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          message: userMessage,
          chatId: chatId // Send connection ID for memory
        }),
      })

      // 응답 상태 확인
      if (!response.ok) {
        throw new Error('Network response was not ok')
      }

      // JSON 데이터 파싱 및 봇 응답 표시
      const data = await response.json()
      setMessages(prev => [...prev, { text: data.response, isUser: false }])
    } catch (error) {
      // 에러 처리: 콘솔에 로그를 남기고 사용자에게 에러 메시지 표시
      console.error('Error:', error)
      setMessages(prev => [...prev, { text: '죄송합니다. 오류가 발생했습니다.', isUser: false }])
    } finally {
      // 로딩 상태 해제
      setIsLoading(false)
    }
  }

  const handleSubmit = (e) => {
    e.preventDefault()
    sendMessage(input)
  }

  const handleNewChat = () => {
    setMessages([])
    setInput('')
    const newChatId = crypto.randomUUID()
    setChatId(newChatId)
    setIsSidebarOpen(false) // Close sidebar on mobile if open
  }

  const suggestions = [
    "Spring AI란 무엇인가요?",
    "RAG 시스템 구축 방법 알려줘",
    "Java 최신 기능 요약해줘",
    "오늘의 날씨는?"
  ]

  return (
    <div className="app-container">
      {/* Sidebar */}
      <aside className={`sidebar ${isSidebarOpen ? 'open' : ''}`}>
        <div className="sidebar-header">
          <span>Gemini Pro</span>
          <button className="menu-btn" onClick={() => setIsSidebarOpen(false)}>✕</button>
        </div>
        <button className="new-chat-btn" onClick={handleNewChat}>
          <span>+</span> 새로운 채팅
        </button>
        {/* Chat History placeholders could go here */}
        <div className="history-section">
          <div className="history-label">최근 활동</div>
          {/* Mock items */}
          <div className="history-item">Spring AI 프로젝트 설정</div>
          <div className="history-item">Java 스트림 API 예제</div>
        </div>
      </aside>

      {/* Main Content */}
      <main className="main-content">
        {/* Header */}
        <header className="chat-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', width: '100%' }}>
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
            {apiHealth !== null && (
              <div style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: '6px', fontSize: '13px', color: apiHealth ? '#4caf50' : '#f44336', fontWeight: 500, paddingRight: '1rem' }}>
                <div style={{ width: '8px', height: '8px', borderRadius: '50%', backgroundColor: apiHealth ? '#4caf50' : '#f44336' }}></div>
                {apiHealth ? 'API 연결됨' : 'API 연결 실패'}
              </div>
            )}
          </div>
        </header>

        {/* Chat Area */}
        <div className="messages-area">
          {messages.length === 0 ? (
            <div className="welcome-section">
              <h1 className="welcome-title">
                <span className="gradient-text">안녕하세요, User님</span>
                <br />
                <span className="sub-text">오늘 무엇을 도와드릴까요?</span>
              </h1>
              <div className="suggestions-grid">
                {suggestions.map((text, index) => (
                  <button key={index} className="suggestion-card" onClick={() => sendMessage(text)}>
                    <p>{text}</p>
                    <div className="icon-wrapper">
                      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                        <path d="M12 4L12 20" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
                        <path d="M18 14L12 20L6 14" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
                        <path d="M4 4L20 4" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
                      </svg> {/* Simple icon, can be replaced */}
                    </div>
                  </button>
                ))}
              </div>
            </div>
          ) : (
            <>
              {messages.map((msg, index) => (
                <div key={index} className={`message-wrapper ${msg.isUser ? 'user-msg' : 'bot-msg'}`}>
                  {/* Avatar for Bot only */}
                  {!msg.isUser && (
                    <div className="message-icon bot-icon">
                      <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
                        <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 17.93c-3.95-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2v1.93zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39z" />
                      </svg>
                    </div>
                  )}

                  <div className="message-content">
                    {/* Only show sender name for Bot to keep User clean */}
                    {!msg.isUser && <div className="message-sender">Gemini</div>}
                    <div className="text-bubble">
                      {msg.text}
                    </div>
                  </div>
                </div>
              ))}

              {isLoading && (
                <div className="message-wrapper bot-msg">
                  <div className="message-icon bot-icon">
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 17.93c-3.95-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2v1.93zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39z" />
                    </svg>
                  </div>
                  <div className="message-content">
                    <div className="message-sender">Gemini</div>
                    <div className="loading-dots">
                      <div className="dot"></div>
                      <div className="dot"></div>
                      <div className="dot"></div>
                    </div>
                  </div>
                </div>
              )}
              <div ref={messagesEndRef} />
            </>
          )}
        </div>

        {/* Footer / Input Area */}
        <div className="input-area-wrapper">
          <form className="input-container" onSubmit={handleSubmit}>
            <input
              type="text"
              className="chat-input"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder="여기에 프롬프트 입력"
              disabled={isLoading}
            />
            <button type="submit" className="send-btn" disabled={isLoading || !input.trim()}>
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M2.01 21L23 12L2.01 3L2 10L17 12L2 14L2.01 21Z" fill="currentColor" />
              </svg>
            </button>
          </form>
          <div className="footer-info">
            Gemini는 실수를 할 수 있습니다. 중요한 정보는 확인해 주세요.
          </div>
        </div>
      </main>

      {/* Overlay for mobile */}
      {isSidebarOpen && (
        <div
          className="mobile-overlay"
          onClick={() => setIsSidebarOpen(false)}
        />
      )}
    </div>
  )
}

export default App

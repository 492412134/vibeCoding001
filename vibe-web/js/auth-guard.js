/**
 * 登录守卫 - 检查登录状态并引导用户登录
 */

const AuthGuard = {
    /**
     * 检查登录状态，未登录时显示提示
     * @param {Object} options - 配置选项
     * @param {boolean} options.showModal - 是否显示登录弹框，默认为true
     * @param {string} options.message - 自定义提示消息
     * @param {Function} options.onLogin - 登录成功回调
     * @param {Function} options.onCancel - 取消登录回调
     * @returns {Promise<boolean>} - 是否已登录
     */
    async checkLogin(options = {}) {
        const { 
            showModal = true, 
            message = '该功能需要登录后才能使用',
            onLogin,
            onCancel 
        } = options;

        // 已登录直接返回true
        if (AuthService.isLoggedIn()) {
            this.updateUIForLoggedIn();
            return true;
        }

        // 未登录，显示提示
        if (showModal) {
            // 显示登录引导弹框
            const result = await this.showLoginPrompt(message);
            
            if (result.success) {
                this.updateUIForLoggedIn();
                if (onLogin) onLogin();
                return true;
            } else {
                if (onCancel) onCancel();
                return false;
            }
        }

        return false;
    },

    /**
     * 显示登录引导弹框
     */
    showLoginPrompt(message) {
        return new Promise((resolve) => {
            // 创建弹框
            const modal = document.createElement('div');
            modal.id = 'auth-prompt-modal';
            modal.innerHTML = `
                <div class="auth-prompt-overlay">
                    <div class="auth-prompt-content">
                        <div class="auth-prompt-icon">🔒</div>
                        <h3>需要登录</h3>
                        <p class="auth-prompt-message">${message}</p>
                        <div class="auth-prompt-actions">
                            <a href="/login.html?redirect=${encodeURIComponent(window.location.pathname)}" class="btn btn-primary">
                                去登录
                            </a>
                            <button class="btn btn-secondary" id="auth-prompt-cancel">稍后再说</button>
                        </div>
                    </div>
                </div>
            `;

            // 添加样式
            const style = document.createElement('style');
            style.id = 'auth-prompt-style';
            style.textContent = `
                .auth-prompt-overlay {
                    position: fixed;
                    top: 0;
                    left: 0;
                    right: 0;
                    bottom: 0;
                    background: rgba(0, 0, 0, 0.7);
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    z-index: 10000;
                    animation: fadeIn 0.3s ease;
                }
                .auth-prompt-content {
                    background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
                    border: 2px solid #0f3460;
                    border-radius: 12px;
                    padding: 40px;
                    text-align: center;
                    max-width: 400px;
                    width: 90%;
                    box-shadow: 0 10px 40px rgba(0, 0, 0, 0.5);
                    animation: slideUp 0.3s ease;
                }
                .auth-prompt-icon {
                    font-size: 48px;
                    margin-bottom: 16px;
                }
                .auth-prompt-content h3 {
                    color: #fff;
                    margin: 0 0 12px 0;
                    font-size: 20px;
                }
                .auth-prompt-message {
                    color: #a0a0a0;
                    margin-bottom: 24px;
                    font-size: 14px;
                    line-height: 1.5;
                }
                .auth-prompt-actions {
                    display: flex;
                    gap: 12px;
                    justify-content: center;
                }
                .auth-prompt-actions .btn {
                    padding: 12px 24px;
                    border-radius: 6px;
                    font-size: 14px;
                    cursor: pointer;
                    text-decoration: none;
                    transition: all 0.3s;
                }
                .auth-prompt-actions .btn-primary {
                    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                    color: #fff;
                    border: none;
                }
                .auth-prompt-actions .btn-primary:hover {
                    transform: translateY(-2px);
                    box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
                }
                .auth-prompt-actions .btn-secondary {
                    background: transparent;
                    color: #888;
                    border: 1px solid #444;
                }
                .auth-prompt-actions .btn-secondary:hover {
                    border-color: #666;
                    color: #aaa;
                }
                @keyframes fadeIn {
                    from { opacity: 0; }
                    to { opacity: 1; }
                }
                @keyframes slideUp {
                    from { transform: translateY(20px); opacity: 0; }
                    to { transform: translateY(0); opacity: 1; }
                }
            `;

            document.head.appendChild(style);
            document.body.appendChild(modal);

            // 绑定事件
            modal.querySelector('#auth-prompt-cancel').addEventListener('click', () => {
                this.hideLoginPrompt();
                resolve({ success: false, cancelled: true });
            });

            modal.querySelector('.auth-prompt-overlay').addEventListener('click', (e) => {
                if (e.target === modal.querySelector('.auth-prompt-overlay')) {
                    this.hideLoginPrompt();
                    resolve({ success: false, cancelled: true });
                }
            });

            // 监听登录成功事件
            const checkLoginInterval = setInterval(() => {
                if (AuthService.isLoggedIn()) {
                    clearInterval(checkLoginInterval);
                    this.hideLoginPrompt();
                    resolve({ success: true });
                }
            }, 1000);
        });
    },

    /**
     * 隐藏登录引导弹框
     */
    hideLoginPrompt() {
        const modal = document.getElementById('auth-prompt-modal');
        const style = document.getElementById('auth-prompt-style');
        if (modal) modal.remove();
        if (style) style.remove();
    },

    /**
     * 更新UI为已登录状态
     */
    updateUIForLoggedIn() {
        // 查找登录提示元素并移除
        const loginHints = document.querySelectorAll('.login-hint');
        loginHints.forEach(el => el.remove());

        // 启用需要登录的按钮
        const protectedButtons = document.querySelectorAll('[data-require-login]');
        protectedButtons.forEach(btn => {
            btn.disabled = false;
            btn.title = '';
        });

        // 显示用户信息
        this.showUserInfo();
    },

    /**
     * 显示用户信息
     */
    showUserInfo() {
        const userInfo = AuthService.getUserInfo();
        if (!userInfo) return;

        // 移除已有的用户信息栏
        const existingBar = document.getElementById('user-info-bar');
        if (existingBar) existingBar.remove();

        const userBar = document.createElement('div');
        userBar.id = 'user-info-bar';
        userBar.innerHTML = `
            <span class="user-greeting">👋 欢迎，${userInfo.username}</span>
            <span class="user-role">${userInfo.role}</span>
            <button class="btn-logout" onclick="AuthGuard.logout()">退出</button>
        `;

        const style = document.createElement('style');
        style.textContent = `
            #user-info-bar {
                position: fixed;
                top: 20px;
                right: 20px;
                background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                padding: 10px 20px;
                border-radius: 25px;
                display: flex;
                align-items: center;
                gap: 12px;
                z-index: 9999;
                box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
            }
            .user-greeting {
                color: #fff;
                font-size: 14px;
            }
            .user-role {
                background: rgba(255, 255, 255, 0.2);
                color: #fff;
                padding: 2px 8px;
                border-radius: 10px;
                font-size: 12px;
            }
            .btn-logout {
                background: rgba(255, 255, 255, 0.2);
                color: #fff;
                border: none;
                padding: 4px 12px;
                border-radius: 12px;
                cursor: pointer;
                font-size: 12px;
                transition: background 0.3s;
            }
            .btn-logout:hover {
                background: rgba(255, 255, 255, 0.3);
            }
        `;

        document.head.appendChild(style);
        document.body.appendChild(userBar);
    },

    /**
     * 退出登录
     */
    logout() {
        AuthService.logout();
    },

    /**
     * 包装需要登录的函数
     * @param {Function} fn - 需要登录才能执行的函数
     * @param {Object} options - 配置选项
     */
    requireLogin(fn, options = {}) {
        return async (...args) => {
            const isLoggedIn = await this.checkLogin(options);
            if (isLoggedIn) {
                return fn(...args);
            }
        };
    },

    /**
     * 初始化页面登录状态
     */
    init() {
        if (AuthService.isLoggedIn()) {
            this.updateUIForLoggedIn();
        } else {
            // 禁用需要登录的按钮
            const protectedButtons = document.querySelectorAll('[data-require-login]');
            protectedButtons.forEach(btn => {
                btn.disabled = true;
                btn.title = '请先登录';
            });
        }
    }
};

// 自动初始化
document.addEventListener('DOMContentLoaded', () => {
    AuthGuard.init();
});

if (typeof window !== 'undefined') {
    window.AuthGuard = AuthGuard;
}

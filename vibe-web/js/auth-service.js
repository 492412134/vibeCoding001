/**
 * 认证服务 - 处理登录、Token管理和请求拦截
 */

const AuthService = {
    TOKEN_KEY: 'vibe_access_token',
    USER_INFO_KEY: 'vibe_user_info',

    _loginCallbacks: [],
    _isShowingLogin: false,

    getToken() {
        return localStorage.getItem(this.TOKEN_KEY);
    },

    setToken(accessToken) {
        localStorage.setItem(this.TOKEN_KEY, accessToken);
    },

    clearToken() {
        localStorage.removeItem(this.TOKEN_KEY);
        localStorage.removeItem(this.USER_INFO_KEY);
    },

    isLoggedIn() {
        return !!this.getToken();
    },

    getUserInfo() {
        const info = localStorage.getItem(this.USER_INFO_KEY);
        return info ? JSON.parse(info) : null;
    },

    setUserInfo(userInfo) {
        localStorage.setItem(this.USER_INFO_KEY, JSON.stringify(userInfo));
    },

    async login(username, password) {
        try {
            const response = await fetch('/api/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
                body: JSON.stringify({ username, password })
            });

            const result = await response.json();

            if (response.ok && result.code === 200 && result.data) {
                const { accessToken, userInfo } = result.data;
                this.setToken(accessToken);
                if (userInfo) {
                    this.setUserInfo(userInfo);
                }
                return { success: true, data: result.data };
            }

            return { success: false, message: result.message || '登录失败' };
        } catch (error) {
            console.error('登录请求失败:', error);
            return { success: false, message: '网络错误，请稍后重试' };
        }
    },

    async logout() {
        const token = this.getToken();

        if (token) {
            try {
                await fetch('/api/auth/logout', {
                    method: 'POST',
                    headers: { 'Authorization': 'Bearer ' + token },
                    credentials: 'include'
                });
            } catch (e) {
                console.log('登出请求失败，忽略');
            }
        }

        this.clearToken();
        window.location.href = '/login.html';
    },

    async refreshToken() {
        try {
            const response = await fetch('/api/auth/refresh', {
                method: 'POST',
                credentials: 'include'
            });

            const result = await response.json();

            if (response.ok && result.code === 200 && result.data) {
                this.setToken(result.data.accessToken);
                return { success: true };
            }

            return { success: false };
        } catch (error) {
            console.error('刷新Token失败:', error);
            return { success: false };
        }
    },

    showLoginModal() {
        if (this._isShowingLogin) {
            return new Promise((resolve) => {
                this._loginCallbacks.push(resolve);
            });
        }

        this._isShowingLogin = true;

        const modal = document.createElement('div');
        modal.id = 'login-modal';
        modal.innerHTML = '<div class="login-modal-overlay"><div class="login-modal-content"><div class="login-modal-header"><h3>请先登录</h3><button class="login-modal-close">&times;</button></div><div class="login-modal-body"><p class="login-modal-tip">该操作需要登录，请登录后继续</p><form id="login-modal-form"><div class="form-group"><label>用户名</label><input type="text" id="modal-username" placeholder="请输入用户名" required></div><div class="form-group"><label>密码</label><input type="password" id="modal-password" placeholder="请输入密码" required></div><div class="form-error" id="modal-error"></div><div class="form-actions"><button type="submit" class="btn btn-primary">登录</button><a href="/login.html" class="btn btn-link">去登录页面</a></div></form></div></div></div>';

        const style = document.createElement('style');
        style.id = 'login-modal-style';
        style.textContent = '.login-modal-overlay{position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(0,0,0,0.5);display:flex;align-items:center;justify-content:center;z-index:9999}.login-modal-content{background:#fff;border-radius:8px;width:90%;max-width:400px;box-shadow:0 4px 20px rgba(0,0,0,0.15)}.login-modal-header{display:flex;justify-content:space-between;align-items:center;padding:16px 20px;border-bottom:1px solid #e8e8e8}.login-modal-header h3{margin:0;font-size:18px}.login-modal-close{background:none;border:none;font-size:24px;cursor:pointer;color:#999}.login-modal-close:hover{color:#333}.login-modal-body{padding:20px}.login-modal-tip{color:#666;margin-bottom:16px;font-size:14px}.form-group{margin-bottom:16px}.form-group label{display:block;margin-bottom:6px;font-size:14px;color:#333}.form-group input{width:100%;padding:10px 12px;border:1px solid #d9d9d9;border-radius:4px;font-size:14px;box-sizing:border-box}.form-group input:focus{outline:none;border-color:#1890ff}.form-error{color:#ff4d4f;font-size:13px;margin-bottom:12px;min-height:20px}.form-actions{display:flex;justify-content:space-between;align-items:center}.btn{padding:10px 20px;border-radius:4px;font-size:14px;cursor:pointer;border:none}.btn-primary{background:#1890ff;color:#fff}.btn-primary:hover{background:#40a9ff}.btn-link{background:none;color:#1890ff;text-decoration:none}.btn-link:hover{text-decoration:underline}';

        document.head.appendChild(style);
        document.body.appendChild(modal);

        const closeModal = () => {
            if (document.getElementById('login-modal')) {
                document.getElementById('login-modal').remove();
            }
            if (document.getElementById('login-modal-style')) {
                document.getElementById('login-modal-style').remove();
            }
            this._isShowingLogin = false;
        };

        const resolveCallbacks = (result) => {
            this._loginCallbacks.forEach(cb => cb(result));
            this._loginCallbacks = [];
        };

        modal.querySelector('.login-modal-close').addEventListener('click', () => {
            closeModal();
            resolveCallbacks({ success: false, cancelled: true });
        });

        modal.querySelector('.login-modal-overlay').addEventListener('click', (e) => {
            if (e.target === modal.querySelector('.login-modal-overlay')) {
                closeModal();
                resolveCallbacks({ success: false, cancelled: true });
            }
        });

        modal.querySelector('#login-modal-form').addEventListener('submit', async (e) => {
            e.preventDefault();
            const username = document.getElementById('modal-username').value;
            const password = document.getElementById('modal-password').value;
            const errorEl = document.getElementById('modal-error');

            const result = await this.login(username, password);

            if (result.success) {
                closeModal();
                resolveCallbacks({ success: true });
                window.location.reload();
            } else {
                errorEl.textContent = result.message || '用户名或密码错误';
            }
        });

        return new Promise((resolve) => {
            this._loginCallbacks.push(resolve);
        });
    },

    hideLoginModal() {
        if (document.getElementById('login-modal')) {
            document.getElementById('login-modal').remove();
        }
        if (document.getElementById('login-modal-style')) {
            document.getElementById('login-modal-style').remove();
        }
        this._isShowingLogin = false;
    }
};

const Api = {
    _retryCount: 0,
    MAX_RETRY: 1,

    async request(url, options = {}) {
        const token = AuthService.getToken();
        const defaultOptions = {
            headers: {
                'Content-Type': 'application/json',
                ...(token ? { 'Authorization': 'Bearer ' + token } : {})
            },
            credentials: 'include'
        };

        const finalOptions = {
            ...defaultOptions,
            ...options,
            headers: {
                ...defaultOptions.headers,
                ...options.headers
            }
        };

        try {
            const response = await fetch(url, finalOptions);

            if (response.status === 401) {
                if (this._retryCount < this.MAX_RETRY) {
                    this._retryCount++;
                    const refreshResult = await AuthService.refreshToken();
                    if (refreshResult.success) {
                        finalOptions.headers['Authorization'] = 'Bearer ' + AuthService.getToken();
                        return this.request(url, finalOptions);
                    }
                }

                this._retryCount = 0;
                await AuthService.showLoginModal();
                return { status: 401, needLogin: true };
            }

            this._retryCount = 0;
            return response;
        } catch (error) {
            console.error('请求失败:', error);
            throw error;
        }
    },

    async get(url) {
        const response = await this.request(url, { method: 'GET' });
        return response.json ? response.json() : response;
    },

    async post(url, data) {
        const response = await this.request(url, {
            method: 'POST',
            body: JSON.stringify(data)
        });
        return response.json ? response.json() : response;
    },

    async put(url, data) {
        const response = await this.request(url, {
            method: 'PUT',
            body: JSON.stringify(data)
        });
        return response.json ? response.json() : response;
    },

    async delete(url) {
        const response = await this.request(url, { method: 'DELETE' });
        return response.json ? response.json() : response;
    }
};

function initAuth() {
    const userInfo = AuthService.getUserInfo();
    if (userInfo) {
        console.log('当前用户:', userInfo.username, '角色:', userInfo.role);
    }
}

if (typeof window !== 'undefined') {
    window.AuthService = AuthService;
    window.Api = Api;
    window.initAuth = initAuth;

    document.addEventListener('DOMContentLoaded', () => {
        initAuth();
    });
}

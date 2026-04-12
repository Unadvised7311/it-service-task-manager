import React, { useState, useEffect, useCallback } from 'react';

const API_ORIGIN = (process.env.REACT_APP_API_URL || 'http://localhost:8081').replace(/\/api\/?$/, '').replace(/\/$/, '');
const API = `${API_ORIGIN}/api`;
const TOKEN_KEY = 'taskmanager_token';
const USER_KEY = 'taskmanager_user';

const ROLE_LABELS = {
    ADMIN: 'Administrator:in',
    PROJECT_LEAD: 'Projektleiter:in',
    MEMBER: 'Mitarbeitende:r',
};

const STATUS_LABELS = {
    OPEN: 'Offen',
    IN_PROGRESS: 'In Bearbeitung',
    DONE: 'Erledigt',
};

function loadStoredSession() {
    try {
        const token = localStorage.getItem(TOKEN_KEY);
        const raw = localStorage.getItem(USER_KEY);
        if (!token || !raw) return null;
        const user = JSON.parse(raw);
        return { token, user };
    } catch {
        return null;
    }
}

function saveSession(token, user) {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(USER_KEY, JSON.stringify(user));
}

function clearSession() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
}

function App() {
    const [session, setSession] = useState(() => loadStoredSession());
    const [view, setView] = useState('dashboard');
    const [loginForm, setLoginForm] = useState({ username: '', password: '' });
    const [loginError, setLoginError] = useState('');
    const [projects, setProjects] = useState([]);
    const [tasks, setTasks] = useState([]);
    const [assignableUsers, setAssignableUsers] = useState([]);
    const [adminUsers, setAdminUsers] = useState([]);
    const [selectedProject, setSelectedProject] = useState(null);
    const [newProject, setNewProject] = useState({ name: '', description: '' });
    const [editProject, setEditProject] = useState({ name: '', description: '' });
    const [newTask, setNewTask] = useState({ title: '', description: '' });
    const [editingTask, setEditingTask] = useState(null);
    const [newUser, setNewUser] = useState({ username: '', password: '', role: 'MEMBER', email: '' });
    const [busy, setBusy] = useState(false);
    const [toast, setToast] = useState('');
    const [formError, setFormError] = useState('');
    const [memberConfig, setMemberConfig] = useState({});
    const [inviteEmail, setInviteEmail] = useState('');
    const [inviteAccess, setInviteAccess] = useState('WRITE');
    const [pendingInviteToken, setPendingInviteToken] = useState(() => {
        try {
            return new URLSearchParams(window.location.search).get('invite') || '';
        } catch {
            return '';
        }
    });
    const [invitePreview, setInvitePreview] = useState(null);
    const [inviteForm, setInviteForm] = useState({ username: '', password: '' });
    const [inviteBusy, setInviteBusy] = useState(false);
    const [inviteErr, setInviteErr] = useState('');

    const token = session?.token;
    const currentUser = session?.user;

    const callApi = useCallback(
        async (path, options = {}) => {
            const headers = {
                ...(options.body ? { 'Content-Type': 'application/json' } : {}),
                ...options.headers,
            };
            if (token) headers.Authorization = `Bearer ${token}`;
            const res = await fetch(`${API}${path}`, { ...options, headers });
            if (res.status === 401) {
                clearSession();
                setSession(null);
                throw new Error('Nicht angemeldet');
            }
            if (res.status === 403) {
                throw new Error('Keine Berechtigung');
            }
            if (!res.ok) {
                let msg = res.statusText;
                try {
                    const err = await res.json();
                    if (err.message) msg = err.message;
                    else if (err.detail) msg = err.detail;
                } catch {
                    /* ignore */
                }
                throw new Error(msg);
            }
            if (res.status === 204) return null;
            const text = await res.text();
            return text ? JSON.parse(text) : null;
        },
        [token]
    );

    const refreshProjects = useCallback(async () => {
        if (!token) return;
        const data = await callApi('/projects');
        setProjects(data);
    }, [token, callApi]);

    useEffect(() => {
        if (!token) return;
        refreshProjects().catch((e) => console.error(e));
    }, [token, refreshProjects]);

    useEffect(() => {
        if (!toast) return undefined;
        const t = setTimeout(() => setToast(''), 4500);
        return () => clearTimeout(t);
    }, [toast]);

    const fetchTasks = async (projectId) => {
        const data = await callApi(`/tasks/project/${projectId}`);
        setTasks(data);
    };

    const fetchAdminUsers = async () => {
        if (!token || currentUser?.role !== 'ADMIN') return;
        const data = await callApi('/admin/users');
        setAdminUsers(data);
    };

    useEffect(() => {
        if (token && currentUser?.role === 'ADMIN') fetchAdminUsers().catch(console.error);
    }, [token, currentUser?.role]);

    useEffect(() => {
        if (!pendingInviteToken || currentUser) {
            setInvitePreview(null);
            return;
        }
        fetch(`${API}/invitations/${encodeURIComponent(pendingInviteToken)}`)
            .then((r) => r.json())
            .then(setInvitePreview)
            .catch(() => setInvitePreview({ valid: false, reason: 'Vorschau nicht ladbar' }));
    }, [pendingInviteToken, currentUser]);

    const handleLogin = async (e) => {
        e.preventDefault();
        setLoginError('');
        setBusy(true);
        try {
            const res = await fetch(`${API}/auth/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(loginForm),
            });
            if (!res.ok) {
                setLoginError('Anmeldung fehlgeschlagen. Bitte Zugangsdaten prüfen.');
                return;
            }
            const data = await res.json();
            saveSession(data.token, data.user);
            setSession({ token: data.token, user: data.user });
            setView('dashboard');
            setLoginForm({ username: '', password: '' });
        } catch {
            setLoginError('Server nicht erreichbar.');
        } finally {
            setBusy(false);
        }
    };

    const handleLogout = () => {
        clearSession();
        setSession(null);
        setSelectedProject(null);
        setView('dashboard');
    };

    const openProject = async (project) => {
        setEditingTask(null);
        setSelectedProject(project);
        setEditProject({ name: project.name, description: project.description || '' });
        setFormError('');
        await fetchTasks(project.id);
        if (currentUser?.role === 'PROJECT_LEAD' || currentUser?.role === 'ADMIN') {
            try {
                const users = await callApi('/users');
                setAssignableUsers(users);
                const cfg = {};
                users.forEach((u) => {
                    const row = (project.members || []).find((m) => m.userId === u.id);
                    cfg[u.id] = {
                        selected: !!row,
                        access: row?.accessLevel === 'READ' ? 'READ' : 'WRITE',
                    };
                });
                setMemberConfig(cfg);
            } catch {
                setAssignableUsers([]);
                setMemberConfig({});
            }
        }
    };

    const createProject = async (e) => {
        e.preventDefault();
        setFormError('');
        try {
            await callApi('/projects', {
                method: 'POST',
                body: JSON.stringify({ name: newProject.name, description: newProject.description }),
            });
            setNewProject({ name: '', description: '' });
            setToast('Projekt angelegt.');
            await refreshProjects();
        } catch (err) {
            setFormError(err.message || 'Projekt konnte nicht angelegt werden.');
        }
    };

    const canDeleteProject = (p) =>
        currentUser?.role === 'ADMIN' || Number(p?.ownerId) === Number(currentUser?.id);

    const deleteProjectById = async (projectId) => {
        if (
            !window.confirm(
                'Projekt endgültig löschen? Alle Aufgaben, Zuordnungen und offenen Einladungen werden entfernt.'
            )
        ) {
            return;
        }
        setFormError('');
        try {
            await callApi(`/projects/${projectId}`, { method: 'DELETE' });
            setToast('Projekt gelöscht.');
            if (Number(selectedProject?.id) === Number(projectId)) {
                setSelectedProject(null);
            }
            await refreshProjects();
        } catch (e) {
            setFormError(e.message || 'Projekt konnte nicht gelöscht werden.');
        }
    };

    const saveProjectMeta = async (e) => {
        e.preventDefault();
        await callApi(`/projects/${selectedProject.id}`, {
            method: 'PUT',
            body: JSON.stringify({
                name: editProject.name,
                description: editProject.description,
                status: selectedProject.status,
            }),
        });
        await refreshProjects();
        const updated = (await callApi('/projects')).find((p) => p.id === selectedProject.id);
        if (updated) setSelectedProject(updated);
    };

    const toggleArchive = async (archived) => {
        await callApi(`/projects/${selectedProject.id}/archive`, {
            method: 'PATCH',
            body: JSON.stringify({ archived }),
        });
        await refreshProjects();
        const list = await callApi('/projects');
        const updated = list.find((p) => p.id === selectedProject.id);
        if (updated) setSelectedProject(updated);
    };

    const saveMembers = async () => {
        setFormError('');
        try {
            const members = assignableUsers
                .filter((u) => memberConfig[u.id]?.selected)
                .map((u) => ({
                    userId: u.id,
                    accessLevel: memberConfig[u.id]?.access === 'READ' ? 'READ' : 'WRITE',
                }));
            await callApi(`/projects/${selectedProject.id}/members`, {
                method: 'PUT',
                body: JSON.stringify({ members }),
            });
            setToast('Zuordnung gespeichert.');
            await refreshProjects();
            const list = await callApi('/projects');
            const updated = list.find((p) => p.id === selectedProject.id);
            if (updated) setSelectedProject(updated);
        } catch (err) {
            setFormError(err.message || 'Zuordnung fehlgeschlagen.');
        }
    };

    const sendInvitation = async (e) => {
        e.preventDefault();
        setFormError('');
        try {
            await callApi(`/projects/${selectedProject.id}/invitations`, {
                method: 'POST',
                body: JSON.stringify({ email: inviteEmail.trim(), accessLevel: inviteAccess }),
            });
            setInviteEmail('');
            setToast('Einladung gesendet. Ohne SMTP erscheint der Link im Server-Log.');
        } catch (err) {
            setFormError(err.message || 'Einladung fehlgeschlagen.');
        }
    };

    const createTask = async (e) => {
        e.preventDefault();
        await callApi('/tasks', {
            method: 'POST',
            body: JSON.stringify({
                title: newTask.title,
                description: newTask.description || '',
                projectId: selectedProject.id,
            }),
        });
        setNewTask({ title: '', description: '' });
        await fetchTasks(selectedProject.id);
        await refreshProjects();
    };

    const saveTaskEdit = async (e) => {
        e.preventDefault();
        await callApi(`/tasks/${editingTask.id}`, {
            method: 'PATCH',
            body: JSON.stringify({
                title: editingTask.title,
                description: editingTask.description || '',
            }),
        });
        setEditingTask(null);
        await fetchTasks(selectedProject.id);
        await refreshProjects();
    };

    const changeTaskStatus = async (taskId, status) => {
        await callApi(`/tasks/${taskId}/status?status=${encodeURIComponent(status)}`, {
            method: 'PATCH',
        });
        await fetchTasks(selectedProject.id);
        await refreshProjects();
    };

    const createUserAdmin = async (e) => {
        e.preventDefault();
        setFormError('');
        try {
            const payload = {
                username: newUser.username,
                password: newUser.password,
                role: newUser.role,
                email: newUser.email?.trim() || null,
            };
            await callApi('/admin/users', {
                method: 'POST',
                body: JSON.stringify(payload),
            });
            setNewUser({ username: '', password: '', role: 'MEMBER', email: '' });
            setToast('Benutzer angelegt.');
            await fetchAdminUsers();
        } catch (err) {
            setFormError(err.message || 'Benutzer konnte nicht angelegt werden.');
        }
    };

    const deleteUserAdmin = async (userId) => {
        if (!window.confirm('Diesen Benutzer wirklich löschen? Projekte der Person werden Ihnen als Projektleitung zugewiesen.')) {
            return;
        }
        try {
            await callApi(`/admin/users/${userId}`, { method: 'DELETE' });
            setToast('Benutzer gelöscht.');
            await fetchAdminUsers();
        } catch (e) {
            setFormError(e.message || 'Löschen fehlgeschlagen.');
        }
    };

    const updateUserRole = async (userId, role) => {
        await callApi(`/admin/users/${userId}/role`, {
            method: 'PATCH',
            body: JSON.stringify({ role }),
        });
        await fetchAdminUsers();
    };

    const acceptInvitation = async (e) => {
        e.preventDefault();
        setInviteErr('');
        setInviteBusy(true);
        try {
            const res = await fetch(`${API}/invitations/${encodeURIComponent(pendingInviteToken)}/accept`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    username: inviteForm.username.trim(),
                    password: inviteForm.password,
                }),
            });
            if (!res.ok) {
                let msg = 'Annahme fehlgeschlagen';
                try {
                    const j = await res.json();
                    if (j.message) msg = j.message;
                    else if (j.detail) msg = j.detail;
                } catch {
                    /* ignore */
                }
                setInviteErr(msg);
                return;
            }
            setPendingInviteToken('');
            window.history.replaceState({}, '', window.location.pathname);
            setToast('Konto erstellt. Bitte anmelden.');
        } catch {
            setInviteErr('Server nicht erreichbar.');
        } finally {
            setInviteBusy(false);
        }
    };

    const isLeadOfSelected =
        selectedProject &&
        (currentUser?.role === 'ADMIN' ||
            (currentUser?.role === 'PROJECT_LEAD' &&
                Number(selectedProject.ownerId) === Number(currentUser.id)));
    const archived = selectedProject?.status === 'ARCHIVED';
    const taskReadOnly =
        archived || (selectedProject && selectedProject.myAccess === 'READ' && currentUser?.role !== 'ADMIN');

    if (!currentUser && pendingInviteToken && invitePreview === null) {
        return (
            <div style={styles.loginScreen}>
                <div style={styles.loginCard}>
                    <p style={{ color: '#718096' }}>Einladung wird geladen…</p>
                </div>
            </div>
        );
    }

    if (!currentUser && pendingInviteToken && invitePreview?.valid) {
        return (
            <div style={styles.loginScreen}>
                <div style={styles.loginCard}>
                    <div style={styles.logo}>✦ Taskmanager</div>
                    <p style={{ color: '#718096', marginBottom: '16px' }}>Einladung annehmen</p>
                    <p style={{ fontSize: '15px', marginBottom: '20px' }}>
                        Projekt: <strong>{invitePreview.projectName}</strong>
                        <br />
                        Rolle im Projekt: {invitePreview.accessLevel === 'READ' ? 'nur Lesen' : 'bearbeiten'}
                    </p>
                    <form onSubmit={acceptInvitation}>
                        <input
                            style={{ ...styles.input, width: '100%', marginBottom: '10px', boxSizing: 'border-box' }}
                            placeholder="Benutzername"
                            value={inviteForm.username}
                            onChange={(e) => setInviteForm({ ...inviteForm, username: e.target.value })}
                            required
                            minLength={2}
                        />
                        <input
                            style={{ ...styles.input, width: '100%', marginBottom: '10px', boxSizing: 'border-box' }}
                            type="password"
                            placeholder="Passwort (mind. 4 Zeichen)"
                            value={inviteForm.password}
                            onChange={(e) => setInviteForm({ ...inviteForm, password: e.target.value })}
                            required
                            minLength={4}
                        />
                        {inviteErr ? <p style={{ color: '#e53e3e', fontSize: '14px' }}>{inviteErr}</p> : null}
                        <button type="submit" style={{ ...styles.btnPrimary, width: '100%', marginTop: '8px' }} disabled={inviteBusy}>
                            {inviteBusy ? '…' : 'Konto anlegen & beitreten'}
                        </button>
                    </form>
                    <button
                        type="button"
                        style={{ ...styles.btnSecondary, width: '100%', marginTop: '12px' }}
                        onClick={() => {
                            setPendingInviteToken('');
                            window.history.replaceState({}, '', window.location.pathname);
                        }}
                    >
                        Abbrechen
                    </button>
                </div>
            </div>
        );
    }

    if (!currentUser && pendingInviteToken && invitePreview && !invitePreview.valid) {
        return (
            <div style={styles.loginScreen}>
                <div style={styles.loginCard}>
                    <p style={{ color: '#e53e3e' }}>{invitePreview.reason || 'Ungültige Einladung'}</p>
                    <button
                        type="button"
                        style={{ ...styles.btnPrimary, width: '100%', marginTop: '16px' }}
                        onClick={() => {
                            setPendingInviteToken('');
                            window.history.replaceState({}, '', window.location.pathname);
                        }}
                    >
                        Zur Anmeldung
                    </button>
                </div>
            </div>
        );
    }

    if (!currentUser) {
        return (
            <div style={styles.loginScreen}>
                <div style={styles.loginScreenMain}>
                    <div style={styles.loginCard}>
                        <div style={styles.logo}>✦ Taskmanager</div>
                        <p style={{ color: '#718096', marginBottom: '24px' }}>Anmeldung</p>
                        <form onSubmit={handleLogin}>
                            <input
                                style={{ ...styles.input, width: '100%', marginBottom: '12px', boxSizing: 'border-box' }}
                                placeholder="Benutzername"
                                value={loginForm.username}
                                onChange={(e) => setLoginForm({ ...loginForm, username: e.target.value })}
                                autoComplete="username"
                                required
                            />
                            <input
                                style={{ ...styles.input, width: '100%', marginBottom: '12px', boxSizing: 'border-box' }}
                                type="password"
                                placeholder="Passwort"
                                value={loginForm.password}
                                onChange={(e) => setLoginForm({ ...loginForm, password: e.target.value })}
                                autoComplete="current-password"
                                required
                            />
                            {loginError ? (
                                <p style={{ color: '#e53e3e', fontSize: '14px', marginBottom: '12px' }}>{loginError}</p>
                            ) : null}
                            <button type="submit" style={{ ...styles.btnPrimary, width: '100%' }} disabled={busy}>
                                {busy ? '…' : 'Anmelden'}
                            </button>
                        </form>
                    </div>
                </div>
                <p style={styles.loginDemoFooter}>
                    Demo-Zugang: Benutzer <code>admin</code>, <code>bob</code> oder <code>charlie</code> — Passwort{' '}
                    <code>password</code>
                </p>
            </div>
        );
    }

    if (selectedProject) {
        const progress = selectedProject.progressPercent ?? 0;
        return (
            <DashboardLayout
                user={currentUser}
                logout={handleLogout}
                view={view}
                setView={setView}
                clearFormError={() => setFormError('')}
            >
                {toast ? <div style={styles.toast}>{toast}</div> : null}
                {formError ? <div style={styles.errBanner}>{formError}</div> : null}
                <button type="button" onClick={() => setSelectedProject(null)} style={styles.btnBack}>
                    ← Zurück
                </button>
                <h2 style={styles.pageTitle}>{selectedProject.name}</h2>
                <p style={{ color: '#718096', marginTop: '-12px' }}>
                    Leitung: {selectedProject.ownerUsername}
                    {archived ? (
                        <span style={{ marginLeft: '12px', color: '#d69e2e' }}>Archiviert</span>
                    ) : null}
                </p>
                <div style={{ height: '10px', background: '#edf2f7', borderRadius: '6px', marginBottom: '8px' }}>
                    <div
                        style={{
                            height: '100%',
                            background: '#48bb78',
                            width: `${progress}%`,
                            borderRadius: '6px',
                            transition: 'width 0.4s ease',
                        }}
                    />
                </div>
                <p style={{ fontSize: '14px', color: '#4a5568', marginBottom: '24px' }}>
                    Fortschritt: {selectedProject.taskDone ?? 0} von {selectedProject.taskTotal ?? 0} Aufgaben erledigt (
                    {progress}%)
                </p>

                {selectedProject.myAccess === 'READ' && !archived ? (
                    <p style={{ color: '#805ad5', marginBottom: '16px', fontSize: '14px' }}>
                        Sie haben in diesem Projekt nur <strong>Leserecht</strong> (keine Aufgaben ändern).
                    </p>
                ) : null}

                {isLeadOfSelected && !archived ? (
                    <div style={styles.glassCard}>
                        <h3 style={{ marginTop: 0 }}>Projekt bearbeiten</h3>
                        <form onSubmit={saveProjectMeta}>
                            <input
                                style={{ ...styles.input, width: '100%', marginBottom: '8px', boxSizing: 'border-box' }}
                                value={editProject.name}
                                onChange={(e) => setEditProject({ ...editProject, name: e.target.value })}
                                required
                            />
                            <textarea
                                style={{
                                    ...styles.input,
                                    width: '100%',
                                    minHeight: '72px',
                                    marginBottom: '8px',
                                    boxSizing: 'border-box',
                                }}
                                value={editProject.description}
                                onChange={(e) => setEditProject({ ...editProject, description: e.target.value })}
                                required
                            />
                            <button type="submit" style={styles.btnPrimary}>
                                Speichern
                            </button>
                        </form>
                        <div style={{ marginTop: '16px', display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
                            <button type="button" style={styles.btnWarn} onClick={() => toggleArchive(true)}>
                                Archivieren
                            </button>
                            <button
                                type="button"
                                style={styles.btnDangerSmall}
                                onClick={() => deleteProjectById(selectedProject.id)}
                            >
                                Projekt löschen
                            </button>
                        </div>
                        <h4 style={{ marginBottom: '8px' }}>Mitarbeitende zuordnen</h4>
                        <p style={{ fontSize: '13px', color: '#718096' }}>
                            Nur Benutzer mit Rolle „Mitarbeitende:r“. Pro Person: Lesen oder Bearbeiten.
                        </p>
                        <div style={{ marginBottom: '12px' }}>
                            {assignableUsers.map((u) => (
                                <div
                                    key={u.id}
                                    style={{ display: 'flex', alignItems: 'center', gap: '12px', flexWrap: 'wrap', margin: '8px 0' }}
                                >
                                    <label style={{ minWidth: '160px', cursor: 'pointer' }}>
                                        <input
                                            type="checkbox"
                                            checked={!!memberConfig[u.id]?.selected}
                                            onChange={() => {
                                                setMemberConfig((prev) => ({
                                                    ...prev,
                                                    [u.id]: {
                                                        selected: !prev[u.id]?.selected,
                                                        access: prev[u.id]?.access || 'WRITE',
                                                    },
                                                }));
                                            }}
                                        />{' '}
                                        {u.username}
                                    </label>
                                    {memberConfig[u.id]?.selected ? (
                                        <select
                                            value={memberConfig[u.id]?.access || 'WRITE'}
                                            onChange={(e) =>
                                                setMemberConfig((prev) => ({
                                                    ...prev,
                                                    [u.id]: { selected: true, access: e.target.value },
                                                }))
                                            }
                                            style={styles.select}
                                        >
                                            <option value="WRITE">Bearbeiten</option>
                                            <option value="READ">Nur lesen</option>
                                        </select>
                                    ) : null}
                                </div>
                            ))}
                        </div>
                        <button type="button" style={styles.btnSecondary} onClick={() => saveMembers()}>
                            Zuordnung speichern
                        </button>
                        <h4 style={{ marginTop: '24px', marginBottom: '8px' }}>Per E-Mail einladen</h4>
                        <p style={{ fontSize: '13px', color: '#718096' }}>
                            Link zur Registrierung wird per SMTP versendet; ohne <code>MAIL_HOST</code> steht der Link im
                            Server-Log.
                        </p>
                        <form onSubmit={sendInvitation} style={{ ...styles.formRow, flexWrap: 'wrap', marginTop: '10px' }}>
                            <input
                                style={styles.input}
                                type="email"
                                placeholder="E-Mail"
                                value={inviteEmail}
                                onChange={(e) => setInviteEmail(e.target.value)}
                                required
                            />
                            <select
                                style={styles.select}
                                value={inviteAccess}
                                onChange={(e) => setInviteAccess(e.target.value)}
                            >
                                <option value="WRITE">Als Bearbeitende:r</option>
                                <option value="READ">Nur Lesen</option>
                            </select>
                            <button type="submit" style={styles.btnPrimary}>
                                Einladung senden
                            </button>
                        </form>
                    </div>
                ) : null}

                {isLeadOfSelected && archived ? (
                    <div style={styles.glassCard}>
                        <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
                            <button type="button" style={styles.btnSecondary} onClick={() => toggleArchive(false)}>
                                Aus Archiv holen
                            </button>
                            <button
                                type="button"
                                style={styles.btnDangerSmall}
                                onClick={() => deleteProjectById(selectedProject.id)}
                            >
                                Projekt löschen
                            </button>
                        </div>
                    </div>
                ) : null}

                {!taskReadOnly ? (
                    <form onSubmit={createTask} style={{ ...styles.formRow, flexWrap: 'wrap', marginBottom: '20px' }}>
                        <input
                            style={styles.input}
                            placeholder="Neue Aufgabe (Titel)"
                            value={newTask.title}
                            onChange={(e) => setNewTask({ ...newTask, title: e.target.value })}
                            required
                        />
                        <input
                            style={styles.input}
                            placeholder="Beschreibung (optional)"
                            value={newTask.description}
                            onChange={(e) => setNewTask({ ...newTask, description: e.target.value })}
                        />
                        <button style={styles.btnPrimary} type="submit">
                            Aufgabe anlegen
                        </button>
                    </form>
                ) : archived ? (
                    <p style={{ color: '#718096' }}>Archivierte Projekte sind schreibgeschützt.</p>
                ) : null}

                <div style={{ marginTop: '12px' }}>
                    {tasks.map((t) => (
                        <div key={t.id} style={styles.taskCard}>
                            {editingTask?.id === t.id && !taskReadOnly ? (
                                <form onSubmit={saveTaskEdit}>
                                    <input
                                        style={{ ...styles.input, width: '100%', marginBottom: '6px', boxSizing: 'border-box' }}
                                        value={editingTask.title}
                                        onChange={(e) => setEditingTask({ ...editingTask, title: e.target.value })}
                                        required
                                    />
                                    <input
                                        style={{ ...styles.input, width: '100%', marginBottom: '6px', boxSizing: 'border-box' }}
                                        value={editingTask.description}
                                        onChange={(e) => setEditingTask({ ...editingTask, description: e.target.value })}
                                    />
                                    <button type="submit" style={styles.btnPrimary}>
                                        OK
                                    </button>
                                    <button
                                        type="button"
                                        style={{ ...styles.btnSecondary, marginLeft: '8px' }}
                                        onClick={() => setEditingTask(null)}
                                    >
                                        Abbrechen
                                    </button>
                                </form>
                            ) : (
                                <>
                                    <div style={{ display: 'flex', justifyContent: 'space-between', gap: '12px', flexWrap: 'wrap' }}>
                                        <div>
                                            <strong>{t.title}</strong>
                                            {t.description ? (
                                                <p style={{ margin: '6px 0 0', color: '#4a5568', fontSize: '14px' }}>
                                                    {t.description}
                                                </p>
                                            ) : null}
                                        </div>
                                        {!taskReadOnly ? (
                                            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                                <select
                                                    value={t.status}
                                                    onChange={(e) =>
                                                        changeTaskStatus(t.id, e.target.value).catch(alert)
                                                    }
                                                    style={styles.select}
                                                >
                                                    {Object.entries(STATUS_LABELS).map(([k, v]) => (
                                                        <option key={k} value={k}>
                                                            {v}
                                                        </option>
                                                    ))}
                                                </select>
                                                <button
                                                    type="button"
                                                    style={styles.btnSecondary}
                                                    onClick={() =>
                                                        setEditingTask({
                                                            id: t.id,
                                                            title: t.title,
                                                            description: t.description || '',
                                                        })
                                                    }
                                                >
                                                    Bearbeiten
                                                </button>
                                            </div>
                                        ) : (
                                            <span style={{ fontSize: '14px', color: '#718096' }}>
                                                {STATUS_LABELS[t.status] || t.status}
                                            </span>
                                        )}
                                    </div>
                                </>
                            )}
                        </div>
                    ))}
                </div>
            </DashboardLayout>
        );
    }

    return (
        <DashboardLayout
            user={currentUser}
            logout={handleLogout}
            view={view}
            setView={setView}
            clearFormError={() => setFormError('')}
        >
            {toast ? <div style={styles.toast}>{toast}</div> : null}
            {formError ? <div style={styles.errBanner}>{formError}</div> : null}
            {view === 'admin' && currentUser.role === 'ADMIN' ? (
                <div>
                    <h2 style={styles.pageTitle}>Benutzer &amp; Rollen</h2>
                    <div style={styles.glassCard}>
                        <h3 style={{ marginTop: 0 }}>Neues Benutzerkonto</h3>
                        <form onSubmit={createUserAdmin} style={{ ...styles.formRow, flexWrap: 'wrap' }}>
                            <input
                                style={styles.input}
                                placeholder="Benutzername"
                                value={newUser.username}
                                onChange={(e) => setNewUser({ ...newUser, username: e.target.value })}
                                required
                            />
                            <input
                                style={styles.input}
                                type="password"
                                placeholder="Passwort"
                                value={newUser.password}
                                onChange={(e) => setNewUser({ ...newUser, password: e.target.value })}
                                required
                            />
                            <input
                                style={styles.input}
                                type="email"
                                placeholder="E-Mail (optional)"
                                value={newUser.email}
                                onChange={(e) => setNewUser({ ...newUser, email: e.target.value })}
                            />
                            <select
                                style={styles.select}
                                value={newUser.role}
                                onChange={(e) => setNewUser({ ...newUser, role: e.target.value })}
                            >
                                <option value="MEMBER">Mitarbeitende:r</option>
                                <option value="PROJECT_LEAD">Projektleiter:in</option>
                                <option value="ADMIN">Administrator:in</option>
                            </select>
                            <button type="submit" style={styles.btnPrimary}>
                                Anlegen
                            </button>
                        </form>
                    </div>
                    <div style={styles.glassCard}>
                        <h3 style={{ marginTop: 0 }}>Bestehende Konten</h3>
                        <table style={styles.table}>
                            <thead>
                                <tr>
                                    <th style={styles.th}>Name</th>
                                    <th style={styles.th}>Rolle</th>
                                    <th style={styles.th}>Aktion</th>
                                </tr>
                            </thead>
                            <tbody>
                                {adminUsers.map((u) => (
                                    <tr key={u.id}>
                                        <td style={styles.td}>{u.username}</td>
                                        <td style={styles.td}>
                                            <select
                                                value={u.role}
                                                onChange={(e) =>
                                                    updateUserRole(u.id, e.target.value).catch(alert)
                                                }
                                                style={styles.select}
                                            >
                                                <option value="MEMBER">Mitarbeitende:r</option>
                                                <option value="PROJECT_LEAD">Projektleiter:in</option>
                                                <option value="ADMIN">Administrator:in</option>
                                            </select>
                                        </td>
                                        <td style={styles.td}>
                                            {Number(u.id) === Number(currentUser.id) ? null : (
                                                <button
                                                    type="button"
                                                    style={styles.btnDangerSmall}
                                                    onClick={() => deleteUserAdmin(u.id)}
                                                >
                                                    Löschen
                                                </button>
                                            )}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </div>
            ) : (
                <>
                    <h2 style={styles.pageTitle}>Meine Projekte</h2>
                    <p style={{ color: '#718096', marginTop: '-8px' }}>
                        Es werden nur Projekte angezeigt, für die Sie berechtigt sind.
                    </p>

                    {currentUser.role === 'PROJECT_LEAD' || currentUser.role === 'ADMIN' ? (
                        <div style={styles.glassCard}>
                            <h3 style={{ marginTop: 0 }}>Neues Projekt</h3>
                            <form onSubmit={createProject} style={{ ...styles.formRow, flexWrap: 'wrap' }}>
                                <input
                                    style={styles.input}
                                    placeholder="Name"
                                    value={newProject.name}
                                    onChange={(e) => setNewProject({ ...newProject, name: e.target.value })}
                                    required
                                />
                                <input
                                    style={styles.input}
                                    placeholder="Beschreibung"
                                    value={newProject.description}
                                    onChange={(e) => setNewProject({ ...newProject, description: e.target.value })}
                                    required
                                />
                                <button type="submit" style={styles.btnPrimary}>
                                    Anlegen
                                </button>
                            </form>
                        </div>
                    ) : null}

                    <div style={styles.projectGrid}>
                        {projects.map((p) => (
                            <div key={p.id} style={styles.projectCard}>
                                <h3>{p.name}</h3>
                                <p style={{ color: '#4a5568', fontSize: '14px' }}>{p.description}</p>
                                {p.status === 'ARCHIVED' ? (
                                    <span style={{ fontSize: '12px', color: '#d69e2e' }}>Archiviert</span>
                                ) : null}
                                <div style={{ marginTop: '10px', fontSize: '13px', color: '#718096' }}>
                                    Fortschritt: {p.progressPercent ?? 0}% ({p.taskDone ?? 0}/{p.taskTotal ?? 0} erledigt)
                                </div>
                                <button type="button" onClick={() => openProject(p).catch(alert)} style={styles.btnSecondaryFull}>
                                    Öffnen
                                </button>
                                {canDeleteProject(p) ? (
                                    <button
                                        type="button"
                                        onClick={() => deleteProjectById(p.id)}
                                        style={{ ...styles.btnDangerSmall, width: '100%', marginTop: '8px' }}
                                    >
                                        Löschen
                                    </button>
                                ) : null}
                            </div>
                        ))}
                    </div>
                    {projects.length === 0 ? (
                        <p style={{ color: '#718096' }}>Keine Projekte sichtbar.</p>
                    ) : null}
                </>
            )}
        </DashboardLayout>
    );
}

const DashboardLayout = ({ user, logout, view, setView, clearFormError, children }) => (
    <div style={styles.appLayout}>
        <aside style={styles.sidebar}>
            <div style={styles.sidebarLogo}>✦ Taskmanager</div>
            <nav style={{ marginBottom: '24px' }}>
                <button
                    type="button"
                    onClick={() => {
                        clearFormError?.();
                        setView('dashboard');
                    }}
                    style={view === 'dashboard' ? styles.navBtnActive : styles.navBtn}
                >
                    Projekte
                </button>
                {user.role === 'ADMIN' ? (
                    <button
                        type="button"
                        onClick={() => {
                            clearFormError?.();
                            setView('admin');
                        }}
                        style={view === 'admin' ? styles.navBtnActive : styles.navBtn}
                    >
                        Administration
                    </button>
                ) : null}
            </nav>
            <div style={styles.userInfo}>
                Angemeldet als:
                <br />
                <strong>{user.username}</strong>
                <div style={{ fontSize: '12px', opacity: 0.85, marginTop: '6px' }}>
                    {ROLE_LABELS[user.role] || user.role}
                </div>
            </div>
            <button type="button" onClick={logout} style={styles.btnLogout}>
                Abmelden
            </button>
        </aside>
        <main style={styles.mainContent}>{children}</main>
    </div>
);

const styles = {
    loginScreen: {
        display: 'flex',
        flexDirection: 'column',
        minHeight: '100vh',
        backgroundColor: '#f7fafc',
    },
    loginScreenMain: {
        flex: 1,
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        width: '100%',
    },
    loginDemoFooter: {
        textAlign: 'center',
        fontSize: '12px',
        color: '#a0aec0',
        padding: '12px 20px 28px',
        margin: 0,
        lineHeight: 1.5,
    },
    loginCard: {
        backgroundColor: '#fff',
        padding: '40px 44px',
        borderRadius: '16px',
        boxShadow: '0 10px 25px rgba(0,0,0,0.06)',
        width: '100%',
        maxWidth: '400px',
        textAlign: 'center',
    },
    logo: { fontSize: '28px', fontWeight: 'bold', color: '#2b6cb0', marginBottom: '8px' },
    appLayout: { display: 'flex', minHeight: '100vh', backgroundColor: '#f7fafc', fontFamily: 'system-ui, sans-serif' },
    sidebar: {
        width: '260px',
        backgroundColor: '#1a202c',
        color: '#fff',
        padding: '20px',
        display: 'flex',
        flexDirection: 'column',
    },
    sidebarLogo: { fontSize: '22px', fontWeight: 'bold', marginBottom: '28px', color: '#63b3ed' },
    navBtn: {
        display: 'block',
        width: '100%',
        textAlign: 'left',
        padding: '10px 12px',
        marginBottom: '8px',
        background: 'transparent',
        color: '#e2e8f0',
        border: '1px solid #4a5568',
        borderRadius: '8px',
        cursor: 'pointer',
    },
    navBtnActive: {
        display: 'block',
        width: '100%',
        textAlign: 'left',
        padding: '10px 12px',
        marginBottom: '8px',
        background: '#2b6cb0',
        color: '#fff',
        border: '1px solid #2b6cb0',
        borderRadius: '8px',
        cursor: 'pointer',
    },
    userInfo: { marginBottom: 'auto', padding: '16px', backgroundColor: '#2d3748', borderRadius: '8px' },
    btnLogout: {
        padding: '10px',
        backgroundColor: '#e53e3e',
        color: 'white',
        border: 'none',
        borderRadius: '6px',
        cursor: 'pointer',
        marginTop: '16px',
    },
    mainContent: { flex: 1, padding: '36px 40px', overflowY: 'auto' },
    pageTitle: { margin: '0 0 16px 0', color: '#2d3748' },
    glassCard: {
        backgroundColor: '#fff',
        padding: '20px',
        borderRadius: '12px',
        marginBottom: '24px',
        border: '1px solid #edf2f7',
    },
    formRow: { display: 'flex', gap: '10px', alignItems: 'center' },
    input: { flex: 1, padding: '10px', border: '1px solid #e2e8f0', borderRadius: '6px' },
    select: { padding: '8px 10px', borderRadius: '6px', border: '1px solid #e2e8f0' },
    btnPrimary: {
        backgroundColor: '#3182ce',
        color: '#fff',
        border: 'none',
        padding: '10px 18px',
        borderRadius: '6px',
        cursor: 'pointer',
    },
    btnSecondary: {
        backgroundColor: '#edf2f7',
        color: '#2d3748',
        border: 'none',
        padding: '8px 14px',
        borderRadius: '6px',
        cursor: 'pointer',
    },
    btnWarn: {
        backgroundColor: '#ed8936',
        color: '#fff',
        border: 'none',
        padding: '10px 16px',
        borderRadius: '6px',
        cursor: 'pointer',
    },
    btnDangerSmall: {
        backgroundColor: '#e53e3e',
        color: '#fff',
        border: 'none',
        padding: '6px 12px',
        borderRadius: '6px',
        cursor: 'pointer',
        fontSize: '13px',
    },
    projectGrid: {
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))',
        gap: '20px',
    },
    projectCard: { backgroundColor: '#fff', padding: '20px', borderRadius: '12px', border: '1px solid #edf2f7' },
    btnSecondaryFull: {
        width: '100%',
        padding: '10px',
        marginTop: '14px',
        backgroundColor: '#edf2f7',
        border: 'none',
        borderRadius: '6px',
        cursor: 'pointer',
    },
    taskCard: {
        padding: '14px 16px',
        backgroundColor: '#fff',
        marginBottom: '10px',
        borderRadius: '8px',
        borderLeft: '4px solid #3182ce',
        border: '1px solid #edf2f7',
        borderLeftWidth: '4px',
    },
    btnBack: { background: 'none', border: 'none', color: '#3182ce', cursor: 'pointer', marginBottom: '16px', fontWeight: 'bold' },
    table: { width: '100%', borderCollapse: 'collapse', fontSize: '14px' },
    th: { textAlign: 'left', padding: '8px', borderBottom: '2px solid #edf2f7' },
    td: { padding: '10px 8px', borderBottom: '1px solid #edf2f7', verticalAlign: 'middle' },
    toast: {
        background: '#c6f6d5',
        color: '#22543d',
        padding: '10px 14px',
        borderRadius: '8px',
        marginBottom: '16px',
        fontSize: '14px',
    },
    errBanner: {
        background: '#fed7d7',
        color: '#9b2c2c',
        padding: '10px 14px',
        borderRadius: '8px',
        marginBottom: '16px',
        fontSize: '14px',
    },
};

export default App;

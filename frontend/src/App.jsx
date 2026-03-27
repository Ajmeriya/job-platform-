import { useEffect, useMemo, useState } from "react";
import { Link, Navigate, Route, Routes, useLocation, useNavigate, useParams } from "react-router-dom";

const TOKEN_KEY = "jwt";

async function callApi(url, options = {}) {
  const token = options.token || localStorage.getItem(TOKEN_KEY) || "";
  const res = await fetch(url, {
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(options.headers || {})
    },
    ...options
  });

  const text = await res.text();
  let body = text;

  try {
    body = text ? JSON.parse(text) : {};
  } catch {
    // keep plain text
  }

  if (!res.ok) {
    const message = typeof body === "string" ? body : body?.message || JSON.stringify(body);
    const error = new Error(message || "Request failed");
    error.status = res.status;
    throw error;
  }

  return body;
}

function parseTokenClaims(token) {
  if (!token) {
    return null;
  }

  try {
    const parts = token.split(".");
    if (parts.length < 2) {
      return null;
    }

    let base64 = parts[1].replace(/-/g, "+").replace(/_/g, "/");
    while (base64.length % 4 !== 0) {
      base64 += "=";
    }
    const json = atob(base64);
    const claims = JSON.parse(json);

    return {
      userId: claims.userId != null ? Number(claims.userId) : null,
      role: claims.role || null,
      email: claims.sub || null
    };
  } catch {
    return null;
  }
}

function normalizeRole(rawRole) {
  if (!rawRole) {
    return "";
  }
  const role = String(rawRole).toUpperCase();
  return role.startsWith("ROLE_") ? role.slice(5) : role;
}

function getFriendlyError(err) {
  if (!err) {
    return "Something went wrong.";
  }

  if (err.status === 401 || err.status === 403) {
    return "Access denied (401/403). Login again. If it continues, set the same jwt.secret in auth-service and job-service.";
  }

  return err.message || "Something went wrong.";
}

function handleAuthFailure(err, navigate) {
  if (err?.status === 401) {
    localStorage.removeItem(TOKEN_KEY);
    navigate("/auth/login", { replace: true });
    return true;
  }

  if (err?.status === 403) {
    return false;
  }

  return false;
}

function AuthFrame({ title, subtitle, children }) {
  return (
    <main className="page auth-page">
      <section className="auth-layout">
        <section className="auth-right reveal" style={{ "--delay": "0.08s" }}>
          <p className="eyebrow">AUTH</p>
          <h2>{title}</h2>
          <p className="subtext">{subtitle}</p>
          {children}
        </section>
      </section>
    </main>
  );
}

function SignUpPage() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [formData, setFormData] = useState({
    name: "",
    email: "",
    password: "",
    role: "CANDIDATE"
  });

  const submit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError("");

    try {
      await callApi("/auth/register", {
        method: "POST",
        body: JSON.stringify(formData)
      });
      navigate("/auth/verify", { state: { email: formData.email } });
    } catch (err) {
      setError(getFriendlyError(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthFrame title="Create account" subtitle="Register and verify email to continue.">
      <form className="form" onSubmit={submit}>
        <label>Name<input required value={formData.name} onChange={(e) => setFormData({ ...formData, name: e.target.value })} /></label>
        <label>Email<input type="email" required value={formData.email} onChange={(e) => setFormData({ ...formData, email: e.target.value })} /></label>
        <label>Password<input type="password" required value={formData.password} onChange={(e) => setFormData({ ...formData, password: e.target.value })} /></label>
        <label>Role
          <select value={formData.role} onChange={(e) => setFormData({ ...formData, role: e.target.value })}>
            <option value="CANDIDATE">Candidate</option>
            <option value="RECURATOR">Recurator</option>
          </select>
        </label>
        {error && <p className="error">{error}</p>}
        <button className="btn primary" type="submit" disabled={loading}>{loading ? "Creating..." : "Create Account"}</button>
      </form>

      <p className="switch-line">Already registered? <Link to="/auth/login">Login</Link></p>
    </AuthFrame>
  );
}

function VerifyPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [ok, setOk] = useState("");
  const [formData, setFormData] = useState({
    email: location.state?.email || "",
    code: ""
  });

  const submit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError("");
    setOk("");

    try {
      await callApi("/auth/verify", {
        method: "POST",
        body: JSON.stringify(formData)
      });
      setOk("Verification complete. Redirecting...");
      setTimeout(() => navigate("/auth/login"), 1000);
    } catch (err) {
      setError(getFriendlyError(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthFrame title="Verify account" subtitle="Enter email and verification code.">
      <form className="form" onSubmit={submit}>
        <label>Email<input type="email" required value={formData.email} onChange={(e) => setFormData({ ...formData, email: e.target.value })} /></label>
        <label>Code<input required value={formData.code} onChange={(e) => setFormData({ ...formData, code: e.target.value })} /></label>
        {error && <p className="error">{error}</p>}
        {ok && <p className="ok">{ok}</p>}
        <button className="btn primary" type="submit" disabled={loading}>{loading ? "Verifying..." : "Verify"}</button>
      </form>
    </AuthFrame>
  );
}

function LoginPage() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [info, setInfo] = useState("");
  const [formData, setFormData] = useState({ email: "", password: "" });

  const submit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError("");
    setInfo("");

    try {
      const data = await callApi("/auth/login", {
        method: "POST",
        body: JSON.stringify(formData)
      });

      if (!data?.token) {
        setError("Token not returned from auth-service.");
        return;
      }

      localStorage.setItem(TOKEN_KEY, data.token);
      const claims = parseTokenClaims(data.token);

      const role = normalizeRole(claims?.role);
      if (role === "RECURATOR" || role === "RECRUITER") {
        navigate("/recurator/dashboard", { replace: true });
      } else {
        navigate("/candidate/jobs", { replace: true });
      }
    } catch (err) {
      setError(getFriendlyError(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthFrame title="Welcome back" subtitle="Login to continue.">
      <form className="form" onSubmit={submit}>
        <label>Email<input type="email" required value={formData.email} onChange={(e) => setFormData({ ...formData, email: e.target.value })} /></label>
        <label>Password<input type="password" required value={formData.password} onChange={(e) => setFormData({ ...formData, password: e.target.value })} /></label>
        {error && <p className="error">{error}</p>}
        {info && <p className="ok">{info}</p>}
        <button className="btn primary" type="submit" disabled={loading}>{loading ? "Signing in..." : "Login"}</button>
      </form>

      <p className="switch-line">Need account? <Link to="/auth/signup">Sign up</Link></p>
    </AuthFrame>
  );
}

function useRecruiterSession() {
  const token = localStorage.getItem(TOKEN_KEY) || "";
  const claims = parseTokenClaims(token);
  const normalizedRole = normalizeRole(claims?.role);
  const isRecruiter = normalizedRole === "RECURATOR" || normalizedRole === "RECRUITER";
  return { token, claims, isRecruiter };
}

function RecruiterNav({ active }) {
  const navigate = useNavigate();

  return (
    <nav className="portal-nav reveal" style={{ "--delay": "0.02s" }}>
      <div className="portal-brand" onClick={() => navigate("/recurator/dashboard")}>
        <span className="brand-dot">AI</span>
        <strong>AI Hiring Recruiter</strong>
      </div>

      <div className="portal-links">
        <button className={`nav-link ${active === "dashboard" ? "active" : ""}`} type="button" onClick={() => navigate("/recurator/dashboard")}>Jobs</button>
        <button className={`nav-link ${active === "create" ? "active" : ""}`} type="button" onClick={() => navigate("/recurator/jobs/create")}>Create</button>
        <button className={`nav-link ${active === "applications" ? "active" : ""}`} type="button" onClick={() => navigate("/recurator/applications")}>Applications</button>
        <button className={`nav-link ${active === "rounds" ? "active" : ""}`} type="button" onClick={() => navigate("/recurator/dashboard")}>Rounds</button>
        <button className="nav-link" type="button" onClick={() => {
          localStorage.removeItem(TOKEN_KEY);
          navigate("/auth/login", { replace: true });
        }}>Logout</button>
      </div>
    </nav>
  );
}

function DashboardPage() {
  const navigate = useNavigate();
  const { token, claims, isRecruiter } = useRecruiterSession();
  const [loading, setLoading] = useState(false);
  const [busyId, setBusyId] = useState(null);
  const [error, setError] = useState("");
  const [jobs, setJobs] = useState([]);
  const [editingJob, setEditingJob] = useState(null);
  const [editData, setEditData] = useState({
    title: "",
    description: "",
    skillsRequired: "",
    location: "",
    experienceRequired: ""
  });

  const stats = useMemo(() => {
    return jobs.reduce(
      (acc, job) => {
        acc.total += 1;
        const status = String(job.status || "DRAFT").toUpperCase();
        if (status === "ACTIVE") {
          acc.active += 1;
        } else if (status === "CLOSED") {
          acc.closed += 1;
        } else {
          acc.draft += 1;
        }
        return acc;
      },
      { total: 0, draft: 0, active: 0, closed: 0 }
    );
  }, [jobs]);

  const loadJobs = async () => {
    setLoading(true);
    setError("");
    try {
      const data = await callApi("/api/jobs", { token });
      const allJobs = Array.isArray(data) ? data : [];
      const ownJobs = claims?.userId ? allJobs.filter((j) => Number(j.createdBy) === Number(claims.userId)) : [];
      setJobs(ownJobs);
    } catch (err) {
        if (handleAuthFailure(err, navigate)) {
          return;
        }
        setError(getFriendlyError(err));
    } finally {
      setLoading(false);
    }
  };

  const openEdit = (job) => {
    setEditingJob(job);
    setEditData({
      title: job.title || "",
      description: job.description || "",
      skillsRequired: Array.isArray(job.skillsRequired) ? job.skillsRequired.join(", ") : "",
      location: job.location || "",
      experienceRequired: job.experienceRequired != null ? String(job.experienceRequired) : "0"
    });
    setError("");
  };

  const cancelEdit = () => {
    setEditingJob(null);
  };

  const saveEdit = async (e) => {
    e.preventDefault();
    if (!editingJob) {
      return;
    }

    setBusyId(editingJob.id);
    setError("");
    try {
      const payload = {
        title: editData.title,
        description: editData.description,
        skillsRequired: editData.skillsRequired.split(",").map((s) => s.trim()).filter(Boolean),
        location: editData.location,
        experienceRequired: Number(editData.experienceRequired)
      };

      const updated = await callApi(`/api/jobs/${editingJob.id}?createdBy=${encodeURIComponent(claims.userId)}`, {
        method: "PUT",
        token,
        body: JSON.stringify(payload)
      });

      setJobs((prev) => prev.map((job) => (job.id === updated.id ? updated : job)));
      setEditingJob(null);
    } catch (err) {
      if (handleAuthFailure(err, navigate)) {
        return;
      }
      setError(getFriendlyError(err));
    } finally {
      setBusyId(null);
    }
  };

  const deleteJob = async (jobId) => {
    const ok = window.confirm("Delete this job? This will also delete configured rounds.");
    if (!ok) {
      return;
    }

    setBusyId(jobId);
    setError("");
    try {
      await callApi(`/api/jobs/${jobId}?createdBy=${encodeURIComponent(claims.userId)}`, {
        method: "DELETE",
        token
      });

      setJobs((prev) => prev.filter((job) => job.id !== jobId));
      if (editingJob?.id === jobId) {
        setEditingJob(null);
      }
    } catch (err) {
      if (handleAuthFailure(err, navigate)) {
        return;
      }
      setError(getFriendlyError(err));
    } finally {
      setBusyId(null);
    }
  };

  const quickUpdateStatus = async (jobId, nextStatus) => {
    setBusyId(jobId);
    setError("");

    try {
      const updated = await callApi(`/api/jobs/${jobId}/status?createdBy=${encodeURIComponent(claims.userId)}`, {
        method: "PATCH",
        token,
        body: JSON.stringify({ status: nextStatus })
      });

      setJobs((prev) => prev.map((job) => (job.id === updated.id ? updated : job)));
      if (editingJob?.id === updated.id) {
        setEditingJob(updated);
      }
    } catch (err) {
      if (handleAuthFailure(err, navigate)) {
        return;
      }
      setError(getFriendlyError(err));
    } finally {
      setBusyId(null);
    }
  };

  useEffect(() => {
    if (!token || !isRecruiter) {
      navigate("/auth/login", { replace: true });
      return;
    }
    loadJobs();
  }, []);

  if (!isRecruiter) {
    return null;
  }

  return (
    <main className="page dashboard-page">
      <RecruiterNav active="dashboard" />
      <header className="dash-head reveal" style={{ "--delay": "0.04s" }}>
        <div>
          <p className="tag">RECURATOR DASHBOARD</p>
          <h1>Your Jobs</h1>
          <p className="subtext">{claims?.email}</p>
          <div className="stats-row">
            <span className="mini-chip">Total: {stats.total}</span>
            <span className="mini-chip">Draft: {stats.draft}</span>
            <span className="mini-chip">Active: {stats.active}</span>
            <span className="mini-chip">Closed: {stats.closed}</span>
          </div>
        </div>
        <div className="head-actions">
          <button className="btn ghost" type="button" onClick={loadJobs} disabled={loading}>{loading ? "Refreshing..." : "Refresh"}</button>
          <button className="btn primary" type="button" onClick={() => navigate("/recurator/jobs/create")}>Create Job</button>
          <button className="btn ghost" type="button" onClick={() => { localStorage.removeItem(TOKEN_KEY); navigate("/auth/login", { replace: true }); }}>Logout</button>
        </div>
      </header>

      {error && <p className="error reveal" style={{ "--delay": "0.08s" }}>{error}</p>}

      <section className="jobs-grid reveal" style={{ "--delay": "0.1s" }}>
        {jobs.map((job) => (
          <article key={job.id} className="job-card">
            <div className="job-top">
              <h3>{job.title}</h3>
              <span className={`chip status-${String(job.status || "DRAFT").toLowerCase()}`}>{job.status}</span>
            </div>

            <p className="job-description">{job.description}</p>

            <div className="job-meta-grid">
              <div className="meta"><strong>Location:</strong> {job.location}</div>
              <div className="meta"><strong>Experience:</strong> {job.experienceRequired} yrs</div>
              <div className="meta meta-full"><strong>Skills:</strong> {(job.skillsRequired || []).join(", ") || "-"}</div>
            </div>

            <div className="job-actions">
              <button className="btn primary" type="button" onClick={() => navigate(`/recurator/jobs/${job.id}/rounds`)}>Configure Rounds</button>
              <button className="btn ghost" type="button" onClick={() => navigate(`/recurator/applications?jobId=${job.id}`)}>View Applications</button>
              <button className="btn ghost" type="button" onClick={() => openEdit(job)} disabled={busyId === job.id}>Edit</button>
              <button className="btn danger" type="button" onClick={() => deleteJob(job.id)} disabled={busyId === job.id}>{busyId === job.id ? "Working..." : "Delete"}</button>
            </div>

            <div className="status-actions">
              <span>Quick status:</span>
              <button className={`chip-btn ${job.status === "DRAFT" ? "on" : ""}`} type="button" onClick={() => quickUpdateStatus(job.id, "DRAFT")} disabled={busyId === job.id}>Draft</button>
              <button className={`chip-btn ${job.status === "ACTIVE" ? "on" : ""}`} type="button" onClick={() => quickUpdateStatus(job.id, "ACTIVE")} disabled={busyId === job.id}>Active</button>
              <button className={`chip-btn ${job.status === "CLOSED" ? "on" : ""}`} type="button" onClick={() => quickUpdateStatus(job.id, "CLOSED")} disabled={busyId === job.id}>Closed</button>
            </div>
          </article>
        ))}

        {!loading && jobs.length === 0 && (
          <article className="empty-state">
            <h3>No jobs yet</h3>
            <p>Create your first job and continue to round configuration.</p>
            <button className="btn primary" type="button" onClick={() => navigate("/recurator/jobs/create")}>Create First Job</button>
          </article>
        )}
      </section>

      {editingJob && (
        <section className="step-card reveal" style={{ "--delay": "0.12s" }}>
          <header className="edit-head">
            <h3>Edit Job #{editingJob.id}</h3>
            <button className="btn ghost" type="button" onClick={cancelEdit}>Close</button>
          </header>

          <form className="form" onSubmit={saveEdit}>
            <label>Title<input required value={editData.title} onChange={(e) => setEditData({ ...editData, title: e.target.value })} /></label>
            <label>Description<textarea rows="4" required value={editData.description} onChange={(e) => setEditData({ ...editData, description: e.target.value })} /></label>
            <label>Skills Required (comma separated)<input value={editData.skillsRequired} onChange={(e) => setEditData({ ...editData, skillsRequired: e.target.value })} /></label>
            <label>Location<input required value={editData.location} onChange={(e) => setEditData({ ...editData, location: e.target.value })} /></label>
            <label>Experience Required (years)<input type="number" min="0" required value={editData.experienceRequired} onChange={(e) => setEditData({ ...editData, experienceRequired: e.target.value })} /></label>
            <div className="row-actions">
              <button className="btn primary" type="submit" disabled={busyId === editingJob.id}>{busyId === editingJob.id ? "Saving..." : "Save Changes"}</button>
              <button className="btn ghost" type="button" onClick={cancelEdit}>Cancel</button>
            </div>
          </form>
        </section>
      )}
    </main>
  );
}

function CandidateNav({ active }) {
  const navigate = useNavigate();
  return (
    <nav className="portal-nav reveal" style={{ "--delay": "0.02s" }}>
      <div className="portal-brand" onClick={() => navigate("/candidate/jobs")}>
        <span className="brand-dot">AI</span>
        <strong>AI Hiring Candidate</strong>
      </div>

      <div className="portal-links">
        <button className={`nav-link ${active === "jobs" ? "active" : ""}`} type="button" onClick={() => navigate("/candidate/jobs")}>Jobs</button>
        <button className={`nav-link ${active === "applications" ? "active" : ""}`} type="button" onClick={() => navigate("/candidate/applications")}>My Applications</button>
        <button className={`nav-link ${active === "profile" ? "active" : ""}`} type="button" onClick={() => navigate("/candidate/profile")}>Profile</button>
        <button className="nav-link" type="button" onClick={() => {
          localStorage.removeItem(TOKEN_KEY);
          navigate("/auth/login", { replace: true });
        }}>Logout</button>
      </div>
    </nav>
  );
}

function formatDateTime(value) {
  if (!value) {
    return "-";
  }
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "-" : date.toLocaleString();
}

async function uploadResumeFile(file, token) {
  const formData = new FormData();
  formData.append("file", file);

  const res = await fetch("/applications/resumes", {
    method: "POST",
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    body: formData
  });

  const text = await res.text();
  let body = text;
  try {
    body = text ? JSON.parse(text) : {};
  } catch {
    // keep plain text
  }

  if (!res.ok) {
    const message = typeof body === "string" ? body : body?.message || JSON.stringify(body);
    const error = new Error(message || "Resume upload failed");
    error.status = res.status;
    throw error;
  }

  return body;
}

function CandidateJobsPage() {
  const navigate = useNavigate();
  const { token, isRecruiter } = useRecruiterSession();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [jobs, setJobs] = useState([]);
  const loadJobs = async () => {
    setLoading(true);
    setError("");
    try {
      const data = await callApi("/api/jobs", { token });
      const all = Array.isArray(data) ? data : [];
      const visible = all.filter((job) => String(job.status || "").toUpperCase() === "ACTIVE");
      setJobs(visible);
    } catch (err) {
      if (handleAuthFailure(err, navigate)) {
        return;
      }
      setError(getFriendlyError(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!token) {
      navigate("/auth/login", { replace: true });
      return;
    }
    if (isRecruiter) {
      navigate("/recurator/dashboard", { replace: true });
      return;
    }
    loadJobs();
  }, []);

  return (
    <main className="page candidate-page">
      <CandidateNav active="jobs" />

      {error && <p className="error reveal" style={{ "--delay": "0.07s" }}>{error}</p>}

      <section className="jobs-grid reveal" style={{ "--delay": "0.1s" }}>
        {jobs.map((job) => (
          <article key={job.id} className="job-card compact-job">
            <div className="job-top">
              <h3>{job.title}</h3>
              <span className="chip status-active">ACTIVE</span>
            </div>
            <p className="job-description">{job.description?.slice(0, 130)}{job.description && job.description.length > 130 ? "..." : ""}</p>
            <div className="meta"><strong>Location:</strong> {job.location}</div>
            <div className="meta"><strong>Experience:</strong> {job.experienceRequired} years</div>
            <button className="btn ghost" type="button" onClick={() => navigate(`/candidate/jobs/${job.id}`)}>View Details</button>
          </article>
        ))}

        {!loading && jobs.length === 0 && (
          <article className="empty-state">
            <h3>No active jobs</h3>
            <p>Recruiters have not published active jobs yet.</p>
          </article>
        )}
      </section>
    </main>
  );
}

function CandidateApplicationsPage() {
  const navigate = useNavigate();
  const { token, claims, isRecruiter } = useRecruiterSession();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [items, setItems] = useState([]);

  const loadApplications = async () => {
    setLoading(true);
    setError("");
    try {
      const data = await callApi("/applications/me", { token });
      setItems(Array.isArray(data) ? data : []);
    } catch (err) {
      if (handleAuthFailure(err, navigate)) {
        return;
      }
      setError(getFriendlyError(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!token) {
      navigate("/auth/login", { replace: true });
      return;
    }
    if (isRecruiter) {
      navigate("/recurator/dashboard", { replace: true });
      return;
    }
    loadApplications();
  }, []);

  return (
    <main className="page candidate-page">
      <CandidateNav active="applications" />

      {error && <p className="error reveal" style={{ "--delay": "0.07s" }}>{error}</p>}

      <section className="jobs-grid reveal" style={{ "--delay": "0.1s" }}>
        {items.map((app) => (
          <article key={app.id} className="job-card">
            <div className="job-top">
              <h3>Application #{app.id}</h3>
              <span className={`chip status-${String(app.status || "applied").toLowerCase()}`}>{app.status}</span>
            </div>
            <div className="meta"><strong>Job ID:</strong> {app.jobId}</div>
            <div className="meta"><strong>Resume:</strong> <a href={app.resumeUrl} target="_blank" rel="noreferrer">Open</a></div>
            <div className="meta"><strong>Resume Review:</strong> {app.resumeStatus} ({app.resumeScore ?? "-"})</div>
            <div className="meta"><strong>Current Round:</strong> {app.currentRound || "-"}</div>
            <div className="meta"><strong>Updated:</strong> {formatDateTime(app.updatedAt)}</div>
          </article>
        ))}

        {!loading && items.length === 0 && (
          <article className="empty-state">
            <h3>No applications yet</h3>
            <p>Go to Open Jobs and apply to start your process.</p>
            <button className="btn primary" type="button" onClick={() => navigate("/candidate/jobs")}>Browse Jobs</button>
          </article>
        )}
      </section>
    </main>
  );
}

function CandidateProfilePage() {
  const navigate = useNavigate();
  const { token, claims, isRecruiter } = useRecruiterSession();

  useEffect(() => {
    if (!token) {
      navigate("/auth/login", { replace: true });
      return;
    }
    if (isRecruiter) {
      navigate("/recurator/dashboard", { replace: true });
    }
  }, []);

  return (
    <main className="page candidate-page">
      <CandidateNav active="profile" />

      <section className="step-card profile-card reveal" style={{ "--delay": "0.05s" }}>
        <p className="tag">PROFILE</p>
        <h2>Candidate Profile</h2>
        <div className="profile-grid">
          <div><span>Email</span><strong>{claims?.email || "-"}</strong></div>
          <div><span>User ID</span><strong>{claims?.userId ?? "-"}</strong></div>
          <div><span>Role</span><strong>{normalizeRole(claims?.role) || "CANDIDATE"}</strong></div>
          <div><span>Status</span><strong>Active Session</strong></div>
        </div>
        <div className="row-actions">
          <button className="btn primary" type="button" onClick={() => navigate("/candidate/jobs")}>Browse Jobs</button>
          <button className="btn ghost" type="button" onClick={() => navigate("/candidate/applications")}>My Applications</button>
        </div>
      </section>
    </main>
  );
}

function RecruiterApplicationsPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { token, isRecruiter } = useRecruiterSession();
  const queryJobId = new URLSearchParams(location.search).get("jobId") || "";
  const [jobIdFilter, setJobIdFilter] = useState(queryJobId);
  const [loading, setLoading] = useState(false);
  const [busyId, setBusyId] = useState(null);
  const [error, setError] = useState("");
  const [apps, setApps] = useState([]);
  const [roundFormById, setRoundFormById] = useState({});

  const loadApplications = async (targetJobId = jobIdFilter) => {
    setLoading(true);
    setError("");
    try {
      const path = targetJobId ? `/applications/jobs/${encodeURIComponent(targetJobId)}` : "/applications";
      const data = await callApi(path, { token });
      const list = Array.isArray(data) ? data : [];
      setApps(list);

      const defaults = {};
      list.forEach((app) => {
        defaults[app.id] = {
          roundType: app.currentRound || "APTITUDE",
          status: "PENDING",
          score: "",
          feedback: ""
        };
      });
      setRoundFormById(defaults);
    } catch (err) {
      if (handleAuthFailure(err, navigate)) {
        return;
      }
      setError(getFriendlyError(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!token || !isRecruiter) {
      navigate("/auth/login", { replace: true });
      return;
    }
    loadApplications(queryJobId);
  }, []);

  const reviewResume = async (id, resumeStatus) => {
    setBusyId(id);
    setError("");
    try {
      const payload = {
        resumeStatus,
        resumeScore: resumeStatus === "SHORTLISTED" ? 78 : 45
      };
      const updated = await callApi(`/applications/${id}/resume-review`, {
        method: "POST",
        token,
        body: JSON.stringify(payload)
      });
      setApps((prev) => prev.map((a) => (a.id === id ? updated : a)));
    } catch (err) {
      if (handleAuthFailure(err, navigate)) {
        return;
      }
      setError(getFriendlyError(err));
    } finally {
      setBusyId(null);
    }
  };

  const updateRound = async (id) => {
    const form = roundFormById[id];
    if (!form) {
      return;
    }
    setBusyId(id);
    setError("");
    try {
      const payload = {
        roundType: form.roundType,
        status: form.status,
        score: form.score === "" ? null : Number(form.score),
        feedback: form.feedback || null
      };
      const updated = await callApi(`/applications/${id}/rounds`, {
        method: "POST",
        token,
        body: JSON.stringify(payload)
      });
      setApps((prev) => prev.map((a) => (a.id === id ? updated : a)));
    } catch (err) {
      if (handleAuthFailure(err, navigate)) {
        return;
      }
      setError(getFriendlyError(err));
    } finally {
      setBusyId(null);
    }
  };

  return (
    <main className="page recruiter-apps-page">
      <RecruiterNav active="applications" />
      <header className="dash-head reveal" style={{ "--delay": "0.04s" }}>
        <div>
          <p className="tag">RECURATOR OPS</p>
          <h1>Application Pipeline</h1>
          <p className="subtext">Review resumes and move candidates round-by-round.</p>
        </div>
        <div className="head-actions">
          <input className="filter-input" placeholder="Filter by Job ID" value={jobIdFilter} onChange={(e) => setJobIdFilter(e.target.value)} />
          <button className="btn ghost" type="button" onClick={() => loadApplications(jobIdFilter)} disabled={loading}>{loading ? "Loading..." : "Apply Filter"}</button>
          <button className="btn primary" type="button" onClick={() => { setJobIdFilter(""); loadApplications(""); }} disabled={loading}>All</button>
        </div>
      </header>

      {error && <p className="error reveal" style={{ "--delay": "0.07s" }}>{error}</p>}

      <section className="jobs-grid reveal" style={{ "--delay": "0.1s" }}>
        {apps.map((app) => {
          const form = roundFormById[app.id] || { roundType: app.currentRound || "APTITUDE", status: "PENDING", score: "", feedback: "" };
          return (
            <article key={app.id} className="job-card app-card">
              <div className="job-top">
                <h3>Candidate: {app.candidateEmail}</h3>
                <span className={`chip status-${String(app.status || "applied").toLowerCase()}`}>{app.status}</span>
              </div>
              <div className="meta"><strong>Application ID:</strong> {app.id}</div>
              <div className="meta"><strong>Job ID:</strong> {app.jobId}</div>
              <div className="meta"><strong>Current Round:</strong> {app.currentRound || "-"}</div>
              <div className="meta"><strong>Resume:</strong> <a href={app.resumeUrl} target="_blank" rel="noreferrer">Open Resume</a></div>
              <div className="meta"><strong>Resume Status:</strong> {app.resumeStatus} ({app.resumeScore ?? "-"})</div>

              <div className="status-actions">
                <span>Resume:</span>
                <button className="chip-btn" type="button" onClick={() => reviewResume(app.id, "SHORTLISTED")} disabled={busyId === app.id}>Shortlist</button>
                <button className="chip-btn" type="button" onClick={() => reviewResume(app.id, "REJECTED")} disabled={busyId === app.id}>Reject</button>
              </div>

              <div className="round-mini-form">
                <select value={form.roundType} onChange={(e) => setRoundFormById((prev) => ({ ...prev, [app.id]: { ...form, roundType: e.target.value } }))}>
                  <option value="APTITUDE">APTITUDE</option>
                  <option value="TECHNICAL">TECHNICAL</option>
                  <option value="INTERVIEW">INTERVIEW</option>
                </select>
                <select value={form.status} onChange={(e) => setRoundFormById((prev) => ({ ...prev, [app.id]: { ...form, status: e.target.value } }))}>
                  <option value="PENDING">PENDING</option>
                  <option value="PASSED">PASSED</option>
                  <option value="FAILED">FAILED</option>
                </select>
                <input placeholder="Score" type="number" min="0" max="100" value={form.score} onChange={(e) => setRoundFormById((prev) => ({ ...prev, [app.id]: { ...form, score: e.target.value } }))} />
                <input placeholder="Feedback" value={form.feedback} onChange={(e) => setRoundFormById((prev) => ({ ...prev, [app.id]: { ...form, feedback: e.target.value } }))} />
                <button className="btn primary" type="button" onClick={() => updateRound(app.id)} disabled={busyId === app.id}>{busyId === app.id ? "Saving..." : "Update Round"}</button>
              </div>
            </article>
          );
        })}

        {!loading && apps.length === 0 && (
          <article className="empty-state">
            <h3>No applications found</h3>
            <p>Try removing filters or wait for candidates to apply.</p>
          </article>
        )}
      </section>
    </main>
  );
}

function CreateJobPage() {
  const navigate = useNavigate();
  const { token, claims, isRecruiter } = useRecruiterSession();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [formData, setFormData] = useState({
    title: "",
    description: "",
    skillsRequired: "",
    location: "",
    experienceRequired: ""
  });

  useEffect(() => {
    if (!token || !isRecruiter) {
      navigate("/auth/login", { replace: true });
    }
  }, []);

  const submit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError("");

    try {
      const payload = {
        title: formData.title,
        description: formData.description,
        skillsRequired: formData.skillsRequired.split(",").map((s) => s.trim()).filter(Boolean),
        location: formData.location,
        experienceRequired: Number(formData.experienceRequired)
      };

      const created = await callApi(`/api/jobs?createdBy=${encodeURIComponent(claims.userId)}`, {
        method: "POST",
        body: JSON.stringify(payload),
        token
      });

      navigate(`/recurator/jobs/${created.id}/rounds`, { replace: true });
    } catch (err) {
      if (handleAuthFailure(err, navigate)) {
        return;
      }
      setError(getFriendlyError(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="page step-page">
      <RecruiterNav active="create" />
      <header className="dash-head">
        <div>
          <p className="tag">STEP 1</p>
          <h1>Create Job</h1>
        </div>
        <button className="btn ghost" type="button" onClick={() => navigate("/recurator/dashboard")}>Back to Jobs</button>
      </header>

      <section className="step-card reveal" style={{ "--delay": "0.07s" }}>
        <form className="form" onSubmit={submit}>
          <label>Title<input required value={formData.title} onChange={(e) => setFormData({ ...formData, title: e.target.value })} /></label>
          <label>Description<textarea rows="4" required value={formData.description} onChange={(e) => setFormData({ ...formData, description: e.target.value })} /></label>
          <label>Skills Required (comma separated)<input value={formData.skillsRequired} onChange={(e) => setFormData({ ...formData, skillsRequired: e.target.value })} /></label>
          <label>Location<input required value={formData.location} onChange={(e) => setFormData({ ...formData, location: e.target.value })} /></label>
          <label>Experience Required (years)<input type="number" min="0" required value={formData.experienceRequired} onChange={(e) => setFormData({ ...formData, experienceRequired: e.target.value })} /></label>
          {error && <p className="error">{error}</p>}
          <button className="btn primary" type="submit" disabled={loading}>{loading ? "Creating..." : "Next: Configure Rounds"}</button>
        </form>
      </section>
    </main>
  );
}

const ROUND_ORDER = ["APTITUDE", "TECHNICAL", "INTERVIEW"];

const ROUND_TYPE_CONFIG = {
  APTITUDE: { defaultTime: 30, hint: "Set aptitude questions and topics.", title: "Round 1: Aptitude" },
  TECHNICAL: { defaultTime: 45, hint: "Set DSA/SQL questions and technical topics.", title: "Round 2: Technical" },
  INTERVIEW: { defaultTime: 60, hint: "Set interview skills to evaluate.", title: "Round 3: Interview" }
};

function makeRoundDraft(roundType) {
  return {
    roundType,
    aptitudeQuestions: "",
    aptitudeTopics: "",
    dsaQuestions: "",
    sqlQuestions: "",
    technicalTopics: "",
    difficulty: "MEDIUM",
    timeLimit: String(ROUND_TYPE_CONFIG[roundType]?.defaultTime || 30),
    interviewSkills: ""
  };
}

function mapSavedRoundToForm(round) {
  const type = round?.roundType || "APTITUDE";
  return {
    roundType: type,
    aptitudeQuestions: round?.aptitudeQuestions ?? "",
    aptitudeTopics: Array.isArray(round?.aptitudeTopics) ? round.aptitudeTopics.join(", ") : "",
    dsaQuestions: round?.dsaQuestions ?? "",
    sqlQuestions: round?.sqlQuestions ?? "",
    technicalTopics: Array.isArray(round?.technicalTopics) ? round.technicalTopics.join(", ") : "",
    difficulty: round?.difficulty || "MEDIUM",
    timeLimit: round?.timeLimit != null ? String(round.timeLimit) : String(ROUND_TYPE_CONFIG[type]?.defaultTime || 30),
    interviewSkills: Array.isArray(round?.interviewSkills) ? round.interviewSkills.join(", ") : ""
  };
}

function toRoundPayload(round) {
  return {
    roundType: round.roundType,
    aptitudeQuestions: round.roundType === "APTITUDE" && round.aptitudeQuestions !== "" ? Number(round.aptitudeQuestions) : null,
    aptitudeTopics: round.roundType === "APTITUDE" ? round.aptitudeTopics.split(",").map((s) => s.trim()).filter(Boolean) : [],
    dsaQuestions: round.roundType === "TECHNICAL" && round.dsaQuestions !== "" ? Number(round.dsaQuestions) : null,
    sqlQuestions: round.roundType === "TECHNICAL" && round.sqlQuestions !== "" ? Number(round.sqlQuestions) : null,
    technicalTopics: round.roundType === "TECHNICAL" ? round.technicalTopics.split(",").map((s) => s.trim()).filter(Boolean) : [],
    difficulty: round.difficulty,
    timeLimit: round.timeLimit === "" ? null : Number(round.timeLimit),
    interviewSkills: round.roundType === "INTERVIEW" ? round.interviewSkills.split(",").map((s) => s.trim()).filter(Boolean) : []
  };
}

function ConfigureRoundsPage() {
  const { jobId } = useParams();
  const navigate = useNavigate();
  const { token, claims, isRecruiter } = useRecruiterSession();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [ok, setOk] = useState("");
  const [job, setJob] = useState(null);
  const [stepIndex, setStepIndex] = useState(0);
  const [roundsByType, setRoundsByType] = useState({
    APTITUDE: makeRoundDraft("APTITUDE"),
    TECHNICAL: makeRoundDraft("TECHNICAL"),
    INTERVIEW: makeRoundDraft("INTERVIEW")
  });

  const currentType = ROUND_ORDER[stepIndex];
  const currentRound = roundsByType[currentType];

  const setCurrentField = (field, value) => {
    setRoundsByType((prev) => ({
      ...prev,
      [currentType]: {
        ...prev[currentType],
        [field]: value
      }
    }));
  };

  const init = async () => {
    if (!token || !isRecruiter) {
      navigate("/auth/login", { replace: true });
      return;
    }

    setLoading(true);
    setError("");
    try {
      const loaded = await callApi(`/api/jobs/${jobId}`, { token });
      if (claims?.userId && Number(loaded.createdBy) !== Number(claims.userId)) {
        setError("This job is not created by your account.");
        setJob(null);
        return;
      }
      setJob(loaded);

      const existingRounds = await callApi(`/api/jobs/${jobId}/rounds`, { token });
      const mapped = {
        APTITUDE: makeRoundDraft("APTITUDE"),
        TECHNICAL: makeRoundDraft("TECHNICAL"),
        INTERVIEW: makeRoundDraft("INTERVIEW")
      };

      if (Array.isArray(existingRounds)) {
        existingRounds.forEach((round) => {
          const type = round?.roundType;
          if (type && mapped[type]) {
            mapped[type] = mapSavedRoundToForm(round);
          }
        });
      }

      setRoundsByType(mapped);
    } catch (err) {
      if (handleAuthFailure(err, navigate)) {
        return;
      }
      setError(getFriendlyError(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    init();
  }, [jobId]);

  const validateCurrent = () => {
    if (!currentRound.timeLimit || Number(currentRound.timeLimit) <= 0) {
      return "Time limit must be greater than zero.";
    }

    if (currentType === "APTITUDE") {
      if (!currentRound.aptitudeQuestions || Number(currentRound.aptitudeQuestions) <= 0) {
        return "Aptitude questions must be greater than zero.";
      }
      return "";
    }

    if (currentType === "TECHNICAL") {
      const dsa = currentRound.dsaQuestions === "" ? 0 : Number(currentRound.dsaQuestions);
      const sql = currentRound.sqlQuestions === "" ? 0 : Number(currentRound.sqlQuestions);
      if (dsa < 0 || sql < 0) {
        return "DSA/SQL questions cannot be negative.";
      }
      if (dsa + sql <= 0) {
        return "Add at least one DSA or SQL question.";
      }
      return "";
    }

    if (currentType === "INTERVIEW") {
      const skills = currentRound.interviewSkills.split(",").map((s) => s.trim()).filter(Boolean);
      if (skills.length === 0) {
        return "Interview skills are required.";
      }
    }

    return "";
  };

  const saveAndContinue = async (e) => {
    e.preventDefault();
    setError("");
    setOk("");

    const validationError = validateCurrent();
    if (validationError) {
      setError(validationError);
      return;
    }

    setLoading(true);
    try {
      const payload = ROUND_ORDER.slice(0, stepIndex + 1).map((type) => toRoundPayload(roundsByType[type]));

      await callApi(`/api/jobs/${jobId}/rounds`, {
        method: "POST",
        body: JSON.stringify(payload),
        token
      });

      if (stepIndex < ROUND_ORDER.length - 1) {
        setOk(`${ROUND_TYPE_CONFIG[currentType].title} saved. Continue to next round.`);
        setStepIndex((prev) => prev + 1);
      } else {
        setOk("All rounds configured successfully.");
        setTimeout(() => navigate("/recurator/dashboard", { replace: true }), 900);
      }
    } catch (err) {
      if (handleAuthFailure(err, navigate)) {
        return;
      }
      setError(getFriendlyError(err));
    } finally {
      setLoading(false);
    }
  };

  const goBack = () => {
    setError("");
    setOk("");
    setStepIndex((prev) => Math.max(prev - 1, 0));
  };

  return (
    <main className="page step-page">
      <RecruiterNav active="rounds" />
      <header className="dash-head">
        <div>
          <p className="tag">STEP 2</p>
          <h1>Configure Rounds</h1>
          {job && <p className="subtext">{job.title} (ID: {job.id})</p>}
        </div>
        <button className="btn ghost" type="button" onClick={() => navigate("/recurator/dashboard")}>Back to Jobs</button>
      </header>

      <section className="step-card reveal wizard-shell" style={{ "--delay": "0.07s" }}>
        <div className="wizard-progress">
          {ROUND_ORDER.map((type, index) => (
            <div key={type} className={`wizard-step ${index < stepIndex ? "done" : ""} ${index === stepIndex ? "active" : ""}`}>
              <span>{index + 1}</span>
              <p>{type}</p>
            </div>
          ))}
        </div>

        {error && <p className="error">{error}</p>}
        {ok && <p className="ok">{ok}</p>}

        {job && (
          <form className="stack" onSubmit={saveAndContinue}>
            <section className="round-card">
              <div className="round-head">
                <strong>{ROUND_TYPE_CONFIG[currentType].title}</strong>
                <span className="chip wizard-type-chip">{currentType}</span>
              </div>
              <p className="subtext">{ROUND_TYPE_CONFIG[currentType].hint}</p>

              <label>Difficulty
                <select value={currentRound.difficulty} onChange={(e) => setCurrentField("difficulty", e.target.value)}>
                  <option value="EASY">EASY</option>
                  <option value="MEDIUM">MEDIUM</option>
                  <option value="HARD">HARD</option>
                </select>
              </label>

              <label>Time Limit (minutes)
                <input
                  type="number"
                  min="1"
                  required
                  value={currentRound.timeLimit}
                  onChange={(e) => setCurrentField("timeLimit", e.target.value)}
                />
              </label>

              {currentType === "APTITUDE" && (
                <>
                  <label>Aptitude Questions
                    <input
                      type="number"
                      min="1"
                      required
                      value={currentRound.aptitudeQuestions}
                      onChange={(e) => setCurrentField("aptitudeQuestions", e.target.value)}
                    />
                  </label>
                  <label>Aptitude Topics
                    <input
                      value={currentRound.aptitudeTopics}
                      onChange={(e) => setCurrentField("aptitudeTopics", e.target.value)}
                      placeholder="Logic, Quant, Data Interpretation"
                    />
                  </label>
                </>
              )}

              {currentType === "TECHNICAL" && (
                <>
                  <label>DSA Questions
                    <input
                      type="number"
                      min="0"
                      value={currentRound.dsaQuestions}
                      onChange={(e) => setCurrentField("dsaQuestions", e.target.value)}
                    />
                  </label>
                  <label>SQL Questions
                    <input
                      type="number"
                      min="0"
                      value={currentRound.sqlQuestions}
                      onChange={(e) => setCurrentField("sqlQuestions", e.target.value)}
                    />
                  </label>
                  <label>Technical Topics
                    <input
                      value={currentRound.technicalTopics}
                      onChange={(e) => setCurrentField("technicalTopics", e.target.value)}
                      placeholder="Arrays, Spring Boot, Joins"
                    />
                  </label>
                </>
              )}

              {currentType === "INTERVIEW" && (
                <label>Interview Skills
                  <input
                    required
                    value={currentRound.interviewSkills}
                    onChange={(e) => setCurrentField("interviewSkills", e.target.value)}
                    placeholder="Communication, Problem Solving, Ownership"
                  />
                </label>
              )}
            </section>

            <div className="row-actions wizard-actions">
              <button className="btn ghost" type="button" onClick={goBack} disabled={stepIndex === 0 || loading}>Previous</button>
              <button className="btn primary" type="submit" disabled={loading}>
                {loading ? "Saving..." : stepIndex < ROUND_ORDER.length - 1 ? "Save & Next Round" : "Save & Finish"}
              </button>
            </div>
          </form>
        )}
      </section>
    </main>
  );
}

function CandidateJobDetailsPage() {
  const navigate = useNavigate();
  const { jobId } = useParams();
  const { token, isRecruiter } = useRecruiterSession();
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [job, setJob] = useState(null);
  const [resumeFile, setResumeFile] = useState(null);

  const loadJob = async () => {
    setLoading(true);
    setError("");
    try {
      const byId = await callApi(`/api/jobs/${jobId}`, { token });
      setJob(byId || null);
    } catch (err) {
      // Fallback for environments where the single-job endpoint is unavailable.
      try {
        const list = await callApi("/api/jobs", { token });
        const found = (Array.isArray(list) ? list : []).find((item) => String(item.id) === String(jobId));
        setJob(found || null);
        if (!found) {
          setError("Job not found.");
        }
      } catch (fallbackErr) {
        if (handleAuthFailure(fallbackErr, navigate)) {
          return;
        }
        setError(getFriendlyError(fallbackErr));
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!token) {
      navigate("/auth/login", { replace: true });
      return;
    }
    if (isRecruiter) {
      navigate("/recurator/dashboard", { replace: true });
      return;
    }
    loadJob();
  }, [jobId]);

  const applyToJob = async () => {
    if (!job) {
      return;
    }
    if (!resumeFile) {
      setError("Please upload a resume file before applying.");
      return;
    }

    setBusy(true);
    setError("");
    try {
      const upload = await uploadResumeFile(resumeFile, token);
      const resumeUrl = upload?.fileUrl;

      if (!resumeUrl) {
        throw new Error("Resume upload succeeded but file URL was not returned.");
      }

      await callApi("/applications", {
        method: "POST",
        token,
        body: JSON.stringify({ jobId: job.id, resumeUrl })
      });
      navigate("/candidate/applications");
    } catch (err) {
      if (handleAuthFailure(err, navigate)) {
        return;
      }
      setError(getFriendlyError(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <main className="page candidate-page">
      <CandidateNav active="jobs" />

      <section className="dash-head reveal" style={{ "--delay": "0.05s" }}>
        <div>
          <p className="tag">JOB DETAILS</p>
          <h1>{job?.title || "Job"}</h1>
          <p className="subtext">Review details and apply with your latest resume.</p>
        </div>
        <button className="btn ghost" type="button" onClick={() => navigate("/candidate/jobs")}>Back to Jobs</button>
      </section>

      {error && <p className="error reveal" style={{ "--delay": "0.07s" }}>{error}</p>}

      {loading && <p className="subtext">Loading job details...</p>}

      {!loading && job && (
        <section className="step-card reveal" style={{ "--delay": "0.1s" }}>
          <div className="job-top">
            <h3>{job.title}</h3>
            <span className="chip status-active">{String(job.status || "ACTIVE").toUpperCase()}</span>
          </div>

          <p className="job-description">{job.description || "No description provided."}</p>
          <ul className="job-detail-list">
            <li><strong>Location:</strong> {job.location || "Not specified"}</li>
            <li><strong>Experience Required:</strong> {job.experienceRequired ?? "-"} years</li>
            <li><strong>Skills:</strong> {(job.skillsRequired || []).join(", ") || "Not specified"}</li>
            <li><strong>Created By:</strong> Recruiter #{job.createdBy}</li>
          </ul>

          <label className="inline-label">Upload Resume (PDF/DOC/DOCX)
            <input
              type="file"
              accept=".pdf,.doc,.docx,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
              onChange={(e) => setResumeFile(e.target.files?.[0] || null)}
            />
          </label>
          <p className="meta">Selected: {resumeFile?.name || "No file selected"}</p>

          <div className="row-actions">
            <button className="btn primary" type="button" onClick={applyToJob} disabled={busy || String(job.status || "").toUpperCase() !== "ACTIVE"}>
              {busy ? "Applying..." : "Apply to This Job"}
            </button>
          </div>
        </section>
      )}
    </main>
  );
}

export default function App() {
  return (
    <>
      <Routes>
        <Route path="/" element={<Navigate to="/auth/login" replace />} />
        <Route path="/auth/signup" element={<SignUpPage />} />
        <Route path="/auth/verify" element={<VerifyPage />} />
        <Route path="/auth/login" element={<LoginPage />} />

        <Route path="/recurator/dashboard" element={<DashboardPage />} />
        <Route path="/recurator/jobs/create" element={<CreateJobPage />} />
        <Route path="/recurator/jobs/:jobId/rounds" element={<ConfigureRoundsPage />} />
        <Route path="/recurator/applications" element={<RecruiterApplicationsPage />} />

        <Route path="/candidate/jobs" element={<CandidateJobsPage />} />
        <Route path="/candidate/jobs/:jobId" element={<CandidateJobDetailsPage />} />
        <Route path="/candidate/applications" element={<CandidateApplicationsPage />} />
        <Route path="/candidate/profile" element={<CandidateProfilePage />} />

        <Route path="/jobs" element={<Navigate to="/recurator/dashboard" replace />} />
      </Routes>
    </>
  );
}

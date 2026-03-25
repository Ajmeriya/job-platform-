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
        <aside className="auth-left reveal" style={{ "--delay": "0.04s" }}>
          <p className="tag">AI HIRING</p>
          <h1>Hire Better Talent, Faster</h1>
          <p>
            Recruiter-focused flow for creating jobs and configuring interview rounds in a clean step-by-step journey.
          </p>
        </aside>

        <section className="auth-right reveal" style={{ "--delay": "0.1s" }}>
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
        setInfo("Login successful, but dashboard access is for RECURATOR role only.");
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

function DashboardPage() {
  const navigate = useNavigate();
  const { token, claims, isRecruiter } = useRecruiterSession();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [jobs, setJobs] = useState([]);

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
      <header className="dash-head reveal" style={{ "--delay": "0.04s" }}>
        <div>
          <p className="tag">RECURATOR DASHBOARD</p>
          <h1>Your Jobs</h1>
          <p className="subtext">{claims?.email}</p>
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
            <h3>{job.title}</h3>
            <p>{job.description}</p>
            <div className="meta">{job.location} | Exp {job.experienceRequired} yrs</div>
            <div className="meta">Skills: {(job.skillsRequired || []).join(", ") || "-"}</div>
            <div className="meta">Status: {job.status}</div>
            <button className="btn primary" type="button" onClick={() => navigate(`/recurator/jobs/${job.id}/rounds`)}>Configure Rounds</button>
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

function makeEmptyRound() {
  return {
    roundType: "APTITUDE",
    enabled: true,
    aptitudeQuestions: "",
    aptitudeTopics: "",
    dsaQuestions: "",
    sqlQuestions: "",
    technicalTopics: "",
    difficulty: "MEDIUM",
    timeLimit: "30",
    interviewSkills: ""
  };
}

function mapSavedRoundToForm(round) {
  return {
    roundType: round?.roundType || "APTITUDE",
    enabled: true,
    aptitudeQuestions: round?.aptitudeQuestions ?? "",
    aptitudeTopics: Array.isArray(round?.aptitudeTopics) ? round.aptitudeTopics.join(", ") : "",
    dsaQuestions: round?.dsaQuestions ?? "",
    sqlQuestions: round?.sqlQuestions ?? "",
    technicalTopics: Array.isArray(round?.technicalTopics) ? round.technicalTopics.join(", ") : "",
    difficulty: round?.difficulty || "MEDIUM",
    timeLimit: round?.timeLimit != null ? String(round.timeLimit) : "",
    interviewSkills: Array.isArray(round?.interviewSkills) ? round.interviewSkills.join(", ") : ""
  };
}

const ROUND_TYPE_CONFIG = {
  APTITUDE: { defaultTime: 30, hint: "Set aptitude questions and topics." },
  TECHNICAL: { defaultTime: 45, hint: "Set DSA/SQL questions and technical topics." },
  INTERVIEW: { defaultTime: 60, hint: "Set interview skills to evaluate." }
};

function ConfigureRoundsPage() {
  const { jobId } = useParams();
  const navigate = useNavigate();
  const { token, claims, isRecruiter } = useRecruiterSession();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [ok, setOk] = useState("");
  const [job, setJob] = useState(null);
  const [rounds, setRounds] = useState([makeEmptyRound()]);

  const setRoundField = (index, field, value) => {
    setRounds((prev) => prev.map((round, i) => (i === index ? { ...round, [field]: value } : round)));
  };

  const isTypeTaken = (type, index) => rounds.some((r, i) => i !== index && r.enabled !== false && r.roundType === type);

  const getNextRoundType = (existingRounds) => {
    const taken = new Set(existingRounds.filter((r) => r.enabled !== false).map((r) => r.roundType));
    if (!taken.has("APTITUDE")) {
      return "APTITUDE";
    }
    if (!taken.has("TECHNICAL")) {
      return "TECHNICAL";
    }
    if (!taken.has("INTERVIEW")) {
      return "INTERVIEW";
    }
    return "TECHNICAL";
  };

  const setRoundType = (index, nextType) => {
    setRounds((prev) =>
      prev.map((round, i) => {
        if (i !== index) {
          return round;
        }

        const defaults = ROUND_TYPE_CONFIG[nextType] || { defaultTime: 30 };
        const next = {
          ...round,
          roundType: nextType,
          timeLimit: round.timeLimit || String(defaults.defaultTime)
        };

        if (nextType === "APTITUDE") {
          next.dsaQuestions = "";
          next.sqlQuestions = "";
          next.technicalTopics = "";
          next.interviewSkills = "";
        } else if (nextType === "TECHNICAL") {
          next.aptitudeQuestions = "";
          next.aptitudeTopics = "";
          next.interviewSkills = "";
        } else if (nextType === "INTERVIEW") {
          next.aptitudeQuestions = "";
          next.aptitudeTopics = "";
          next.dsaQuestions = "";
          next.sqlQuestions = "";
          next.technicalTopics = "";
        }

        return next;
      })
    );
  };

  const addRoundCard = () => {
    setRounds((prev) => {
      const activeTypes = new Set(prev.filter((r) => r.enabled !== false).map((r) => r.roundType));
      if (activeTypes.size >= 3) {
        setError("All round types are already active. Disable/remove one to add another.");
        return prev;
      }

      const nextType = getNextRoundType(prev);
      const next = makeEmptyRound();
      next.roundType = nextType;
      next.timeLimit = String(ROUND_TYPE_CONFIG[nextType]?.defaultTime || 30);
      setError("");
      return [...prev, next];
    });
  };
  const removeRoundCard = (index) => setRounds((prev) => prev.filter((_, i) => i !== index));
  const toggleRoundEnabled = (index) => {
    setRounds((prev) => prev.map((round, i) => (i === index ? { ...round, enabled: !round.enabled } : round)));
  };

  useEffect(() => {
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
        if (Array.isArray(existingRounds) && existingRounds.length > 0) {
          setRounds(existingRounds.map(mapSavedRoundToForm));
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

    init();
  }, [jobId]);

  const submit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError("");
    setOk("");

    try {
      const activeRounds = rounds.filter((r) => r.enabled !== false);
      if (activeRounds.length === 0) {
        setError("Enable at least one round before saving.");
        return;
      }

      const payload = activeRounds.map((r) => ({
        roundType: r.roundType,
        aptitudeQuestions: r.roundType === "APTITUDE" && r.aptitudeQuestions !== "" ? Number(r.aptitudeQuestions) : null,
        aptitudeTopics: r.roundType === "APTITUDE" ? r.aptitudeTopics.split(",").map((s) => s.trim()).filter(Boolean) : [],
        dsaQuestions: r.roundType === "TECHNICAL" && r.dsaQuestions !== "" ? Number(r.dsaQuestions) : null,
        sqlQuestions: r.roundType === "TECHNICAL" && r.sqlQuestions !== "" ? Number(r.sqlQuestions) : null,
        technicalTopics: r.roundType === "TECHNICAL" ? r.technicalTopics.split(",").map((s) => s.trim()).filter(Boolean) : [],
        difficulty: r.difficulty,
        timeLimit: r.timeLimit === "" ? null : Number(r.timeLimit),
        interviewSkills: r.roundType === "INTERVIEW" ? r.interviewSkills.split(",").map((s) => s.trim()).filter(Boolean) : []
      }));

      await callApi(`/api/jobs/${jobId}/rounds`, {
        method: "POST",
        body: JSON.stringify(payload),
        token
      });

      setOk("Rounds configured successfully.");
      setTimeout(() => navigate("/recurator/dashboard", { replace: true }), 900);
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
      <header className="dash-head">
        <div>
          <p className="tag">STEP 2</p>
          <h1>Configure Rounds</h1>
          {job && <p className="subtext">{job.title} (ID: {job.id})</p>}
        </div>
        <button className="btn ghost" type="button" onClick={() => navigate("/recurator/dashboard")}>Back to Jobs</button>
      </header>

      <section className="step-card reveal" style={{ "--delay": "0.07s" }}>
        {error && <p className="error">{error}</p>}
        {ok && <p className="ok">{ok}</p>}

        {job && (
          <>
            <div className="row-actions">
              <button className="btn ghost" type="button" onClick={addRoundCard}>Add Round</button>
            </div>

            <form className="stack" onSubmit={submit}>
              {rounds.map((round, index) => (
                <section key={`${index}-${round.roundType}`} className="round-card">
                  <div className="round-head">
                    <strong>Round {index + 1}</strong>
                    <div>
                      <button className="link-btn" type="button" onClick={() => toggleRoundEnabled(index)}>{round.enabled === false ? "Enable" : "Disable"}</button>
                      {rounds.length > 1 && <button className="link-btn" type="button" onClick={() => removeRoundCard(index)}>Remove</button>}
                    </div>
                  </div>

                  {round.enabled === false ? <p className="subtext">This round is disabled and will not be saved.</p> : <p className="subtext">{ROUND_TYPE_CONFIG[round.roundType]?.hint}</p>}

                  <fieldset disabled={round.enabled === false}>
                    <label>Type
                      <select value={round.roundType} onChange={(e) => setRoundType(index, e.target.value)}>
                        <option value="APTITUDE" disabled={round.roundType !== "APTITUDE" && isTypeTaken("APTITUDE", index)}>APTITUDE</option>
                        <option value="TECHNICAL" disabled={round.roundType !== "TECHNICAL" && isTypeTaken("TECHNICAL", index)}>TECHNICAL</option>
                        <option value="INTERVIEW" disabled={round.roundType !== "INTERVIEW" && isTypeTaken("INTERVIEW", index)}>INTERVIEW</option>
                      </select>
                    </label>

                    <label>Difficulty
                      <select value={round.difficulty} onChange={(e) => setRoundField(index, "difficulty", e.target.value)}>
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
                        value={round.timeLimit}
                        onChange={(e) => setRoundField(index, "timeLimit", e.target.value)}
                        placeholder={String(ROUND_TYPE_CONFIG[round.roundType]?.defaultTime || 30)}
                      />
                    </label>

                    {round.roundType === "APTITUDE" && (
                      <>
                        <label>Aptitude Questions<input type="number" min="1" required value={round.aptitudeQuestions} onChange={(e) => setRoundField(index, "aptitudeQuestions", e.target.value)} /></label>
                        <label>Aptitude Topics<input value={round.aptitudeTopics} onChange={(e) => setRoundField(index, "aptitudeTopics", e.target.value)} placeholder="Logic, Quant" /></label>
                      </>
                    )}

                    {round.roundType === "TECHNICAL" && (
                      <>
                        <label>DSA Questions<input type="number" min="0" value={round.dsaQuestions} onChange={(e) => setRoundField(index, "dsaQuestions", e.target.value)} /></label>
                        <label>SQL Questions<input type="number" min="0" value={round.sqlQuestions} onChange={(e) => setRoundField(index, "sqlQuestions", e.target.value)} /></label>
                        <label>Technical Topics<input value={round.technicalTopics} onChange={(e) => setRoundField(index, "technicalTopics", e.target.value)} placeholder="Arrays, Joins" /></label>
                      </>
                    )}

                    {round.roundType === "INTERVIEW" && (
                      <label>Interview Skills<input required value={round.interviewSkills} onChange={(e) => setRoundField(index, "interviewSkills", e.target.value)} placeholder="Communication, Java" /></label>
                    )}
                  </fieldset>
                </section>
              ))}

              <button className="btn primary" type="submit" disabled={loading}>{loading ? "Saving..." : "Save Rounds"}</button>
            </form>
          </>
        )}
      </section>
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

        <Route path="/jobs" element={<Navigate to="/recurator/dashboard" replace />} />
      </Routes>
    </>
  );
}

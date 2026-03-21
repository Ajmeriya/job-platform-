import { useState } from "react";
import { Link, Navigate, Route, Routes, useLocation, useNavigate } from "react-router-dom";

async function callApi(url, options = {}) {
  const res = await fetch(url, {
    headers: {
      "Content-Type": "application/json",
      ...(options.headers || {})
    },
    ...options
  });

  const text = await res.text();
  let parsed = text;

  try {
    parsed = text ? JSON.parse(text) : {};
  } catch {
    // keep plain text if not json
  }

  if (!res.ok) {
    throw new Error(typeof parsed === "string" ? parsed : JSON.stringify(parsed));
  }

  return parsed;
}

function SignUpPage() {
  const navigate = useNavigate();
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState({
    name: "",
    email: "",
    password: "",
    role: "CANDIDATE"
  });

  const onSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      await callApi("/auth/register", {
        method: "POST",
        body: JSON.stringify(formData)
      });

      navigate("/verify", { state: { email: formData.email } });
    } catch (err) {
      setError(err.message || "Register failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="app-shell">
      <section className="single-card reveal" style={{ "--delay": "0.06s" }}>
        <p className="eyebrow">SIGN UP</p>
        <h1>Create Your Account</h1>
        <p className="subtitle">Fill details and get your verification code by email.</p>

        <form className="form" onSubmit={onSubmit}>
          <label>Name<input type="text" required value={formData.name} onChange={(e) => setFormData({ ...formData, name: e.target.value })} /></label>
          <label>Email<input type="email" required value={formData.email} onChange={(e) => setFormData({ ...formData, email: e.target.value })} /></label>
          <label>Password<input type="password" required value={formData.password} onChange={(e) => setFormData({ ...formData, password: e.target.value })} /></label>
          <label>Role
            <select required value={formData.role} onChange={(e) => setFormData({ ...formData, role: e.target.value })}>
              <option value="CANDIDATE">Candidate</option>
              <option value="RECURATOR">Recurator</option>
            </select>
          </label>
          {error && <p className="error-text">{error}</p>}
          <button type="submit" className="btn" disabled={loading}>{loading ? "Sending..." : "Register"}</button>
        </form>

        <p className="switch-auth">Already have an account? <Link to="/login">Login</Link></p>
      </section>
    </main>
  );
}

function VerifyPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState({
    email: location.state?.email || "",
    code: ""
  });

  const onSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setSuccess("");
    setLoading(true);

    try {
      await callApi("/auth/verify", {
        method: "POST",
        body: JSON.stringify(formData)
      });
      setSuccess("Verified successfully. Redirecting to login...");
      setTimeout(() => navigate("/login"), 1200);
    } catch (err) {
      setError(err.message || "Verification failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="app-shell minimal-shell">
      <section className="single-card reveal" style={{ "--delay": "0.06s" }}>
        <p className="eyebrow">VERIFY</p>
        <h1>Enter Verification Code</h1>
        <form className="form" onSubmit={onSubmit}>
          <label>Email<input type="email" required value={formData.email} onChange={(e) => setFormData({ ...formData, email: e.target.value })} /></label>
          <label>Code<input type="text" required value={formData.code} onChange={(e) => setFormData({ ...formData, code: e.target.value })} /></label>
          {error && <p className="error-text">{error}</p>}
          {success && <p className="ok-text">{success}</p>}
          <button type="submit" className="btn" disabled={loading}>{loading ? "Verifying..." : "Verify"}</button>
        </form>
      </section>
    </main>
  );
}

function LoginPage() {
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [token, setToken] = useState("");
  const [formData, setFormData] = useState({ email: "", password: "" });

  const onSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      const data = await callApi("/auth/login", {
        method: "POST",
        body: JSON.stringify(formData)
      });

      if (data?.token) {
        localStorage.setItem("jwt", data.token);
        setToken(data.token);
      }
    } catch (err) {
      setError(err.message || "Login failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="app-shell">
      <section className="single-card reveal" style={{ "--delay": "0.06s" }}>
        <p className="eyebrow">LOGIN</p>
        <h1>Welcome Back</h1>
        <form className="form" onSubmit={onSubmit}>
          <label>Email<input type="email" required value={formData.email} onChange={(e) => setFormData({ ...formData, email: e.target.value })} /></label>
          <label>Password<input type="password" required value={formData.password} onChange={(e) => setFormData({ ...formData, password: e.target.value })} /></label>
          {error && <p className="error-text">{error}</p>}
          <button type="submit" className="btn" disabled={loading}>{loading ? "Logging in..." : "Login"}</button>
        </form>

        <p className="switch-auth">New here? <Link to="/signup">Sign up</Link></p>

        {token && (
          <div className="token-box">
            <p className="token-title">JWT Token</p>
            <textarea rows="5" readOnly value={token} />
          </div>
        )}
      </section>
    </main>
  );
}

export default function App() {
  return (
    <>
      <div className="backdrop-orb orb-1" />
      <div className="backdrop-orb orb-2" />

      <Routes>
        <Route path="/" element={<Navigate to="/signup" replace />} />
        <Route path="/signup" element={<SignUpPage />} />
        <Route path="/verify" element={<VerifyPage />} />
        <Route path="/login" element={<LoginPage />} />
      </Routes>
    </>
  );
}

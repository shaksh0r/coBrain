import { executeCode } from '../API/api'; // Adjust the path if needed

const SendBack = ({ editorRef, language }) => {
  const runCode = async () => {
    console.log("Running Code")
    const sourceCode = editorRef.current.getValue();
    if (!sourceCode) return;
    try {
      const resp = await executeCode(language, sourceCode);
      console.log(resp);
    } catch (error) {
      // Error handling
    }
  };

  return (
    <div>
      <button onClick={runCode}>
        Send Code
      </button>
    </div>
  );
};

export default SendBack;